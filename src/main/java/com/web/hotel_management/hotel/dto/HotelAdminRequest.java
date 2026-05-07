package com.web.hotel_management.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelAdminRequest {

    @NotBlank(message = "Hotel name is required")
    private String name;

    private String address;

    private String description;

    private String phone;

    private String status;

    /** Optional: gán quản lí chi nhánh (UserRole.BRANCH_MANAGER). */
    private Integer managerId;
}

