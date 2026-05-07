package com.web.hotel_management.reception.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ReceptionClientCreateRequest {

    @NotBlank(message = "Họ tên là bắt buộc")
    private String fullName;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải gồm đúng 10 chữ số")
    private String phone;

    @NotBlank(message = "CCCD/Passport là bắt buộc")
    @Pattern(regexp = "^\\d+$", message = "CCCD/Passport chỉ gồm chữ số")
    private String idCardNumber;
}

