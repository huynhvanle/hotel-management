package com.web.hotel_management.user.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SelfUpdateRequest {
    private String fullName;

    @Email(message = "Email must be valid")
    private String mail;

    private String description;

    /** Optional: change password */
    private String password;
}

