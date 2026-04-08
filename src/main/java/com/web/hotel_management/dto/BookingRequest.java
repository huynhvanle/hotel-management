package com.web.hotel_management.dto;

import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {
    private String customerName;
    private String customerEmail;
    private List<Integer> roomIds; 
    private String checkInDate;  
    private String checkOutDate; 
}