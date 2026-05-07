package com.web.hotel_management.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(?i)(ADMIN|BRANCH_MANAGER|RECEPTIONIST)$",
            message = "Role must be ADMIN, BRANCH_MANAGER, or RECEPTIONIST")
    private String role;

    private Integer branchId;

    private String phone;
    
    private String password;
}