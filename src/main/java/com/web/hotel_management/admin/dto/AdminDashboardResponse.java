package com.web.hotel_management.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminDashboardResponse {
    private String from;
    private String to;
    /** DAY | MONTH | YEAR */
    private String groupBy;
    private Double totalRevenue;
    private List<SeriesLine> growth;
    private List<BranchRankItem> ranking;
    /** Optional: compare series for selected branches. */
    private List<BranchSeries> compare;

    @Data
    @Builder
    public static class SeriesLine {
        private String bucket;
        private String label;
        private Double revenue;
    }

    @Data
    @Builder
    public static class BranchRankItem {
        private Integer branchId;
        private String branchName;
        private Double revenue;
        private Integer invoiceCount;
    }

    @Data
    @Builder
    public static class BranchSeries {
        private Integer branchId;
        private String branchName;
        private List<SeriesLine> lines;
    }
}

