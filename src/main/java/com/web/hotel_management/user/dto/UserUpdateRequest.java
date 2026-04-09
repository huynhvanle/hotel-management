package com.web.hotel_management.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    
    @NotBlank(message = "Position is required")
    private String position;
    
    @Email(message = "Email must be valid")
    private String mail;
    
    private String description;
    
    private String password;
}