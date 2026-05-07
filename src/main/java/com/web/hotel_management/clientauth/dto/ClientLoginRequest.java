package com.web.hotel_management.clientauth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClientLoginRequest {
    @NotBlank
    private String phone;

    @NotBlank
    private String password;
}

