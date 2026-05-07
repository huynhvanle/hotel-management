package com.web.hotel_management.reception.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionInvoicePreviewResponse {
    private Integer bookingId;
    private Boolean checkedIn;
    private String clientFullName;
    private String clientPhone;
    private LocalDate checkin;
    private LocalDate checkout;

    private Double totalAmount;
    private Double depositAmount;
    private Double remainingAmount;

    private List<RoomTypeLine> roomTypes;
    private List<String> roomIds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomTypeLine {
        private Integer roomTypeId;
        private String roomTypeName;
        private Integer quantity;
    }
}

