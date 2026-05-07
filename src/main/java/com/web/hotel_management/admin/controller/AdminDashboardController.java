package com.web.hotel_management.admin.controller;

import com.web.hotel_management.admin.dto.AdminDashboardResponse;
import com.web.hotel_management.activity.service.ActivityLogService;
import com.web.hotel_management.invoice.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final InvoiceRepository invoiceRepository;
    private final ActivityLogService activityLogService;

    @GetMapping("/revenue")
    public AdminDashboardResponse revenue(
            @RequestParam(name = "from") LocalDate from,
            @RequestParam(name = "to") LocalDate to,
            @RequestParam(name = "groupBy", defaultValue = "DAY") String groupBy,
            @RequestParam(name = "branchIds", required = false) List<Integer> branchIds,
            Authentication authentication
    ) {
        if (from == null || to == null) throw new RuntimeException("Vui lòng chọn khoảng thời gian.");
        if (to.isBefore(from)) throw new RuntimeException("Khoảng thời gian không hợp lệ.");
        String gb = String.valueOf(groupBy == null ? "DAY" : groupBy).trim().toUpperCase();
        if (!List.of("DAY", "MONTH", "YEAR").contains(gb)) throw new RuntimeException("groupBy không hợp lệ.");

        String u = authentication != null ? authentication.getName() : "unknown";
        log.info("[ADMIN] view dashboard revenue: user={}, from={}, to={}, groupBy={}, branchIds={}", u, from, to, gb, branchIds);
        activityLogService.log(authentication, "ADMIN_DASHBOARD_VIEW", "DASHBOARD", null,
                "from=" + from + ",to=" + to + ",groupBy=" + gb + ",branchIds=" + String.valueOf(branchIds));

        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        // rows: [branchId, branchName, paidAt, paidAmount]
        List<Object[]> rows = invoiceRepository.findPaidInvoiceRowsAllBranchesInRange(fromDt, toDt);

        Map<Integer, BranchAgg> byBranch = new LinkedHashMap<>();
        Map<String, Double> totalBuckets = new LinkedHashMap<>();

        Set<Integer> filterBranches = branchIds == null || branchIds.isEmpty() ? null : new HashSet<>(branchIds);
        for (Object[] r : rows) {
            Integer bid = (Integer) r[0];
            String bname = (String) r[1];
            LocalDateTime paidAt = (LocalDateTime) r[2];
            Double paid = (Double) r[3];
            if (bid == null || paidAt == null) continue;
            if (filterBranches != null && !filterBranches.contains(bid)) continue;
            double amt = paid != null ? paid : 0d;

            BranchAgg agg = byBranch.computeIfAbsent(bid, k -> new BranchAgg(bname));
            agg.sum += amt;
            agg.count += 1;
            String bucket = bucketKey(gb, paidAt.toLocalDate());
            String label = bucketLabel(gb, paidAt.toLocalDate());
            agg.bucketSum.merge(bucket, amt, Double::sum);
            agg.bucketLabel.putIfAbsent(bucket, label);

            totalBuckets.merge(bucket, amt, Double::sum);
        }

        List<AdminDashboardResponse.SeriesLine> growth = totalBuckets.entrySet().stream()
                .map(e -> AdminDashboardResponse.SeriesLine.builder()
                        .bucket(e.getKey())
                        .label(totalLabelForBucket(gb, e.getKey()))
                        .revenue(e.getValue())
                        .build())
                .sorted(Comparator.comparing(AdminDashboardResponse.SeriesLine::getBucket))
                .toList();

        List<AdminDashboardResponse.BranchRankItem> ranking = byBranch.entrySet().stream()
                .map(e -> AdminDashboardResponse.BranchRankItem.builder()
                        .branchId(e.getKey())
                        .branchName(e.getValue().name != null ? e.getValue().name : ("Chi nhánh " + e.getKey()))
                        .revenue(e.getValue().sum)
                        .invoiceCount(e.getValue().count)
                        .build())
                .sorted(Comparator.comparing(AdminDashboardResponse.BranchRankItem::getRevenue).reversed())
                .toList();

        double total = ranking.stream().mapToDouble(x -> x.getRevenue() != null ? x.getRevenue() : 0d).sum();

        List<AdminDashboardResponse.BranchSeries> compare = null;
        if (filterBranches != null && filterBranches.size() >= 2) {
            compare = byBranch.entrySet().stream()
                    .filter(e -> filterBranches.contains(e.getKey()))
                    .map(e -> {
                        BranchAgg a = e.getValue();
                        List<AdminDashboardResponse.SeriesLine> lines = a.bucketSum.entrySet().stream()
                                .map(x -> AdminDashboardResponse.SeriesLine.builder()
                                        .bucket(x.getKey())
                                        .label(a.bucketLabel.getOrDefault(x.getKey(), totalLabelForBucket(gb, x.getKey())))
                                        .revenue(x.getValue())
                                        .build())
                                .sorted(Comparator.comparing(AdminDashboardResponse.SeriesLine::getBucket))
                                .toList();
                        return AdminDashboardResponse.BranchSeries.builder()
                                .branchId(e.getKey())
                                .branchName(a.name != null ? a.name : ("Chi nhánh " + e.getKey()))
                                .lines(lines)
                                .build();
                    })
                    .toList();
        }

        return AdminDashboardResponse.builder()
                .from(from.toString())
                .to(to.toString())
                .groupBy(gb)
                .totalRevenue(total)
                .growth(growth)
                .ranking(ranking)
                .compare(compare)
                .build();
    }

    private static String bucketKey(String gb, LocalDate d) {
        if ("YEAR".equals(gb)) return String.valueOf(d.getYear());
        if ("MONTH".equals(gb)) return String.format("%04d-%02d", d.getYear(), d.getMonthValue());
        return d.toString(); // yyyy-MM-dd
    }

    private static String bucketLabel(String gb, LocalDate d) {
        if ("YEAR".equals(gb)) return String.valueOf(d.getYear());
        if ("MONTH".equals(gb)) return String.format("%02d/%04d", d.getMonthValue(), d.getYear());
        return String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
    }

    private static String totalLabelForBucket(String gb, String bucket) {
        // bucket already stable; label is same for our formats.
        if ("DAY".equals(gb)) {
            // yyyy-MM-dd -> dd/MM/yyyy
            String[] p = bucket.split("-");
            if (p.length == 3) return p[2] + "/" + p[1] + "/" + p[0];
        }
        if ("MONTH".equals(gb)) {
            String[] p = bucket.split("-");
            if (p.length == 2) return p[1] + "/" + p[0];
        }
        return bucket;
    }

    private static class BranchAgg {
        final String name;
        double sum = 0d;
        int count = 0;
        final Map<String, Double> bucketSum = new LinkedHashMap<>();
        final Map<String, String> bucketLabel = new LinkedHashMap<>();
        BranchAgg(String name) { this.name = name; }
    }
}

