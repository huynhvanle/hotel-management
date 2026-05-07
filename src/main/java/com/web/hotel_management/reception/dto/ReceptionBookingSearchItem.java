package com.web.hotel_management.reception.dto;

import com.web.hotel_management.booking.entity.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ReceptionBookingSearchItem {
    private Integer bookingId;
    private String clientPhone;
    private LocalDate checkin;
    private LocalDate checkout;
    private Boolean checkedIn;
    private Boolean checkedOut;
    private BookingStatus status;
    private Double depositAmount;
}

