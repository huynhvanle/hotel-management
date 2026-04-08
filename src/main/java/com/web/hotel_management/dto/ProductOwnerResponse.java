package com.web.hotel_management.dto;

import lombok.Data;

@Data
public class ProductOwnerResponse {
    private Integer id;
    private String name;
    private String email;
    private String phoneNumber;
}