package com.web.hotel_management.booking.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookingResponse {
    private Integer bookingId;
    private Integer bookedRoomId;
}
