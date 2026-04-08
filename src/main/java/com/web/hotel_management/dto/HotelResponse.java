package com.web.hotel_management.dto;

import lombok.Data;

@Data
public class HotelResponse {
    private Integer id;
    private String name;
    private String address;
    private Integer starLevel;
    private String ownerName;
}