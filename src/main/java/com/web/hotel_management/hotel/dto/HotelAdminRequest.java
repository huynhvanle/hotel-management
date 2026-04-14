package com.web.hotel_management.hotel.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Min(value = 1, message = "Star level must be between 1 and 5")
    @Max(value = 5, message = "Star level must be between 1 and 5")
    private Integer starLevel;

    private String address;

    private String description;
}

