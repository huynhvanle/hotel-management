package com.web.hotel_management.roomtype.dto;

import com.web.hotel_management.roomtype.entity.RoomTypeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTypeUpsertRequest {

    @NotBlank(message = "code is required")
    private String code;

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @NotNull(message = "basePrice is required")
    @PositiveOrZero(message = "basePrice must be >= 0")
    private Double basePrice;

    private RoomTypeStatus status;
}

