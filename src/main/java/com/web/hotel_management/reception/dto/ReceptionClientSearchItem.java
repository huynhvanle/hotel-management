package com.web.hotel_management.reception.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReceptionClientSearchItem {
    private Integer id;
    private String fullName;
    private String phone;
    private Long idCardNumber;
}

