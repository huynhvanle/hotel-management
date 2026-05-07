package com.web.hotel_management.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomStaffRequest {

    /** Required for create, ignored on update path param. */
    private String id;

    @NotNull(message = "floor is required")
    private Integer floor;

    @NotBlank(message = "status is required")
    private String status;

    @NotNull(message = "roomTypeId is required")
    private Integer roomTypeId;

    @NotNull(message = "hotelId is required")
    private Integer hotelId;
}

