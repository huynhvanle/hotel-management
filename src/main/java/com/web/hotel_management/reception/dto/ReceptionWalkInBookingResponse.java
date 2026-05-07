package com.web.hotel_management.reception.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionWalkInBookingResponse {
    private Integer bookingId;
    private String message;
}
