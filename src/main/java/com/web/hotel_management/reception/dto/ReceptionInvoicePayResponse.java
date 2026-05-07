package com.web.hotel_management.reception.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionInvoicePayResponse {
    private Integer bookingId;
    private Integer invoiceId;
    private Double paidAmount;
    private LocalDateTime paidAt;
    private String message;
}

