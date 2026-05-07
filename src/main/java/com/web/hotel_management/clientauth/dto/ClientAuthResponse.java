package com.web.hotel_management.clientauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientAuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private ClientProfileResponse client;
}

