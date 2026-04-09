package com.web.hotel_management.auth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "Password must contain at least one letter and one number")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    /** Ánh xạ cột {@code mail} trong {@code tblUser}. */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    /** Khớp cột {@code position} trong {@code tblUser}; mặc định có thể set ở service. */
    private String position;

    private String description;
}
