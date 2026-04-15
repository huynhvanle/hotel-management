package com.web.hotel_management.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueStatsResponse {
    private LocalDate from;
    private LocalDate to;
    /** How rows are filtered */
    private String basis;
    private int distinctBookingCount;
    private int bookedRoomLineCount;
    private double totalRevenue;
    private List<RevenueLineDto> lines;
}
