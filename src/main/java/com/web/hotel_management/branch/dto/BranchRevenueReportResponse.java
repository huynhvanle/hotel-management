package com.web.hotel_management.branch.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BranchRevenueReportResponse {
    private Integer branchId;
    private String branchName;
    /** ISO date range from (inclusive). */
    private String from;
    /** ISO date range to (inclusive). */
    private String to;
    /** DAY | MONTH | YEAR */
    private String groupBy;
    private Double totalRevenue;
    private List<RevenueLine> lines;

    @Data
    @Builder
    public static class RevenueLine {
        /** Label already formatted for UI (e.g. 07/05/2026, 05/2026, 2026). */
        private String label;
        /** Bucket key in ISO-like form for stable sorting (e.g. 2026-05-07, 2026-05, 2026). */
        private String bucket;
        private Double revenue;
        private Integer invoiceCount;
    }
}

