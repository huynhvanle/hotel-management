package com.web.hotel_management.dto;

import lombok.Data;

@Data
public class RoomResponse {
    private Integer id;
    private String type;
    private Double price;
    private String status;
    private String hotelName; 
}