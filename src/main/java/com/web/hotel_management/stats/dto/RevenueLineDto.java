package com.web.hotel_management.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueLineDto {
    private Integer bookingId;
    private String roomId;
    private String roomName;
    private LocalDate bookingDate;
    private LocalDate checkin;
    private LocalDate checkout;
    private long nights;
    /** Room charge (after line discount if any) */
    private double amount;
}
