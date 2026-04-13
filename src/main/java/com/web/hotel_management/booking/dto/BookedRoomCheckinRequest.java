package com.web.hotel_management.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookedRoomCheckinRequest {
    @NotNull(message = "isCheckedIn is required")
    private Integer isCheckedIn; // 0/1
}

