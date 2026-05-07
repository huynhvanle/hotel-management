package com.web.hotel_management.clientauth.dto;

import com.web.hotel_management.client.entity.Client;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientProfileResponse {
    private Integer id;
    private String fullName;
    private String phone;
    private Long idCardNumber;

    public static ClientProfileResponse fromEntity(Client c) {
        if (c == null) return null;
        return ClientProfileResponse.builder()
                .id(c.getId())
                .fullName(c.getFullName())
                .phone(c.getPhone())
                .idCardNumber(c.getIdCardNumber())
                .build();
    }
}

