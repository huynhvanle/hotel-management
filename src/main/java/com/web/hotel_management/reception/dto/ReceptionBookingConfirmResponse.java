package com.web.hotel_management.reception.dto;

import com.web.hotel_management.booking.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionBookingConfirmResponse {
    private Integer bookingId;
    private BookingStatus status;
    private String message;
}

