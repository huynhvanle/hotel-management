package com.web.hotel_management.reception.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReceptionCheckinRequest {
    @NotEmpty(message = "Vui lòng chọn phòng để check-in.")
    private List<String> roomIds;
}

