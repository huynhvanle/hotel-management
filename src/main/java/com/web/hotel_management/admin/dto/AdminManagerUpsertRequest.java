package com.web.hotel_management.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminManagerUpsertRequest {
    @NotBlank(message = "Vui lòng nhập tên đăng nhập.")
    private String username;

    /** Optional on update. */
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự.")
    private String password;

    @NotBlank(message = "Vui lòng nhập họ tên.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "\\d{10}", message = "Số điện thoại phải đúng 10 chữ số.")
    private String phone;

    @NotNull(message = "Vui lòng chọn chi nhánh.")
    private Integer branchId;
}

