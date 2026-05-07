package com.web.hotel_management.clientauth.dto;

import com.web.hotel_management.booking.entity.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ClientBookingItemResponse {
    private Integer bookingId;
    private LocalDate checkin;
    private LocalDate checkout;
    private BookingStatus status;
    private Double depositAmount;
    private Double totalAmount;
    private Boolean checkedIn;
    private List<RoomTypeLine> roomTypes;
    private List<String> roomIds;

    @Data
    @Builder
    public static class RoomTypeLine {
        private Integer roomTypeId;
        private String roomTypeName;
        private Integer quantity;
    }
}

