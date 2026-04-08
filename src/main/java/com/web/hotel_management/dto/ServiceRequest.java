package com.web.hotel_management.dto;

import lombok.Data;

@Data
public class ServiceRequest {
    private Integer bookedRoomId;
    private Integer serviceId;
    private Integer quantity;
}