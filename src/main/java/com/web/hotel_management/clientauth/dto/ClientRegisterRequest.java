package com.web.hotel_management.clientauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClientRegisterRequest {
    @NotBlank(message = "Số điện thoại là bắt buộc")
    private String phone;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 8, max = 100, message = "Mật khẩu phải ít nhất 8 ký tự")
    private String password;

    @NotBlank
    private String fullName;

    private Long idCardNumber;
}

