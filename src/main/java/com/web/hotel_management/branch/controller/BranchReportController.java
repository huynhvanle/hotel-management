package com.web.hotel_management.branch.controller;

import com.web.hotel_management.branch.dto.BranchRevenueReportResponse;
import com.web.hotel_management.activity.service.ActivityLogService;
import com.web.hotel_management.hotel.entity.Hotel;
import com.web.hotel_management.hotel.repository.HotelRepository;
import com.web.hotel_management.invoice.entity.Invoice;
import com.web.hotel_management.invoice.repository.InvoiceRepository;
import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.entity.UserRole;
import com.web.hotel_management.user.repository.UserRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/branch/reports")
@RequiredArgsConstructor
public class BranchReportController {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final InvoiceRepository invoiceRepository;
    private final ActivityLogService activityLogService;

    private Integer resolveBranchId(Authentication authentication, Integer requestedBranchId) {
        String username = authentication != null ? authentication.getName() : null;
        if (username == null || username.isBlank()) throw new RuntimeException("Thiếu thông tin đăng nhập.");
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));

        if (me.getRole() == UserRole.ADMIN) {
            if (requestedBranchId != null) return requestedBranchId;
            Integer bid = me.getBranch() != null ? me.getBranch().getId() : null;
            if (bid == null) throw new RuntimeException("Vui lòng chọn chi nhánh.");
            return bid;
        }

        Integer bid = me.getBranch() != null ? me.getBranch().getId() : null;
        if (bid == null) throw new RuntimeException("Tài khoản chưa được gán chi nhánh.");
        return bid;
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('ADMIN')")
    public BranchRevenueReportResponse revenue(
            @RequestParam(name = "from") LocalDate from,
            @RequestParam(name = "to") LocalDate to,
            @RequestParam(name = "groupBy", defaultValue = "DAY") String groupBy,
            @RequestParam(name = "branchId", required = false) Integer branchId,
            Authentication authentication
    ) {
        Integer resolvedBranchId = resolveBranchId(authentication, branchId);
        String u = authentication != null ? authentication.getName() : "unknown";

        if (from == null || to == null) throw new RuntimeException("Vui lòng chọn khoảng thời gian.");
        if (to.isBefore(from)) throw new RuntimeException("Khoảng thời gian không hợp lệ.");
        String gb = String.valueOf(groupBy == null ? "DAY" : groupBy).trim().toUpperCase();
        if (!List.of("DAY", "MONTH", "YEAR").contains(gb)) throw new RuntimeException("groupBy không hợp lệ.");

        Hotel branch = hotelRepository.findById(resolvedBranchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh."));

        // Paid invoices only (BR2). Range is inclusive by date, so use [from@00:00, to+1@00:00).
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        log.info("[BRANCH] view revenue report: user={}, branchId={}, from={}, to={}, groupBy={}", u, resolvedBranchId, from, to, gb);
        activityLogService.log(authentication, "BRANCH_REVENUE_REPORT_VIEW", "BRANCH", String.valueOf(resolvedBranchId),
                "from=" + from + ",to=" + to + ",groupBy=" + gb);

        List<Invoice> invoices = invoiceRepository.findPaidInvoicesForBranchInRange(resolvedBranchId, fromDt, toDt);

        Map<String, BucketAgg> buckets = new LinkedHashMap<>();
        for (Invoice inv : invoices) {
            LocalDateTime paidAt = inv.getPaidAt();
            if (paidAt == null) continue;
            LocalDate d = paidAt.toLocalDate();
            String bucket;
            String label;
            if ("YEAR".equals(gb)) {
                bucket = String.valueOf(d.getYear());
                label = bucket;
            } else if ("MONTH".equals(gb)) {
                bucket = String.format("%04d-%02d", d.getYear(), d.getMonthValue());
                label = String.format("%02d/%04d", d.getMonthValue(), d.getYear());
            } else {
                bucket = d.toString(); // yyyy-MM-dd
                label = String.format("%02d/%02d/%04d", d.getDayOfMonth(), d.getMonthValue(), d.getYear());
            }
            BucketAgg agg = buckets.computeIfAbsent(bucket, k -> new BucketAgg(label));
            agg.sum += inv.getPaidAmount() != null ? inv.getPaidAmount() : 0d;
            agg.count += 1;
        }

        List<BranchRevenueReportResponse.RevenueLine> lines = buckets.entrySet().stream()
                .map(e -> BranchRevenueReportResponse.RevenueLine.builder()
                        .bucket(e.getKey())
                        .label(e.getValue().label)
                        .revenue(e.getValue().sum)
                        .invoiceCount(e.getValue().count)
                        .build())
                .sorted(Comparator.comparing(BranchRevenueReportResponse.RevenueLine::getBucket))
                .toList();

        double total = lines.stream().mapToDouble(x -> x.getRevenue() != null ? x.getRevenue() : 0d).sum();
        return BranchRevenueReportResponse.builder()
                .branchId(branch.getId())
                .branchName(branch.getName())
                .from(from.toString())
                .to(to.toString())
                .groupBy(gb)
                .totalRevenue(total)
                .lines(lines)
                .build();
    }

    private static class BucketAgg {
        final String label;
        double sum = 0d;
        int count = 0;
        BucketAgg(String label) { this.label = label; }
    }
}

