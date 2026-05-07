package com.web.hotel_management.clientauth.dto;

import com.web.hotel_management.booking.entity.BookingStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientCancelBookingResponse {
    private Integer bookingId;
    private BookingStatus status;
    private String message;
}

