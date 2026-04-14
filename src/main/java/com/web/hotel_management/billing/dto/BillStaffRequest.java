package com.web.hotel_management.billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillStaffRequest {

    @NotNull(message = "bookingId is required")
    private Integer bookingId;

    @NotNull(message = "paymentDate is required")
    private LocalDate paymentDate;

    @NotNull(message = "paymentAmount is required")
    @PositiveOrZero(message = "paymentAmount must be >= 0")
    private Double paymentAmount;

    @NotNull(message = "paymentType is required")
    private Integer paymentType;

    private String note;
}

