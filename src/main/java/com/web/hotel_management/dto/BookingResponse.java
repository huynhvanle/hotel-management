package com.web.hotel_management.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BookingResponse {
    private Integer bookingId;
    private String customerName;
    private double totalAmount;
    private LocalDateTime bookingDate;
    private String message;
    private String status;
    private List<RoomResponse> bookedRooms;
}