package com.web.hotel_management.client.dto;

import com.web.hotel_management.client.entity.Client;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDTO {
    private Integer id;
    private String idCardNumber;
    private String fullName;
    private String address;
    private String email;
    private String phone;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ClientDTO fromEntity(Client client) {
        return ClientDTO.builder()
                .id(client.getId())
                .idCardNumber(client.getIdCardNumber())
                .fullName(client.getFullName())
                .address(client.getAddress())
                .email(client.getEmail())
                .phone(client.getPhone())
                .note(client.getNote())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }
}
