package com.web.hotel_management.reception.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReceptionRoomStatusUpdateRequest {
    @NotBlank(message = "Trạng thái là bắt buộc")
    private String status;
}

