package com.web.hotel_management.reception.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ReceptionWalkInBookingRequest {

    @NotNull(message = "Khách hàng là bắt buộc")
    private Integer clientId;

    @NotNull(message = "Ngày đến là bắt buộc")
    private LocalDate checkin;

    @NotNull(message = "Ngày đi là bắt buộc")
    private LocalDate checkout;

    @NotEmpty(message = "Chọn ít nhất một phòng")
    private List<String> roomIds;
}
