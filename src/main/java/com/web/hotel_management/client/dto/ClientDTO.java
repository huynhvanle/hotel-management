package com.web.hotel_management.client.dto;

import com.web.hotel_management.client.entity.Client;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientDTO {
    private Integer id;
    private Long idCardNumber;
    private String fullName;
    private String address;
    private String email;
    private String phone;
    private String description;

    public static ClientDTO fromEntity(Client client) {
        return ClientDTO.builder()
                .id(client.getId())
                .idCardNumber(client.getIdCardNumber())
                .fullName(client.getFullName())
                .address(client.getAddress())
                .email(client.getEmail())
                .phone(client.getPhone())
                .description(client.getDescription())
                .build();
    }
}
