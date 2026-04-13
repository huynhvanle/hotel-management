package com.web.hotel_management.booking.dto;

import com.web.hotel_management.booking.entity.Booking;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStaffResponse {

    private Integer id;
    private LocalDate bookingDate;
    private Double discount;
    private String note;

    private Integer clientId;
    private String clientName;
    private String clientEmail;
    private String clientPhone;

    private Integer employeeId;
    private String employeeUsername;

    private Integer roomsCount;
    private List<BookedRoomStaffResponse> bookedRooms;

    public static BookingStaffResponse fromEntity(Booking b) {
        if (b == null) return null;
        return BookingStaffResponse.builder()
                .id(b.getId())
                .bookingDate(b.getBookingDate())
                .discount(b.getDiscount())
                .note(b.getNote())
                .clientId(b.getClient() != null ? b.getClient().getId() : null)
                .clientName(b.getClient() != null ? b.getClient().getFullName() : null)
                .clientEmail(b.getClient() != null ? b.getClient().getEmail() : null)
                .clientPhone(b.getClient() != null ? b.getClient().getPhone() : null)
                .employeeId(b.getEmployee() != null ? b.getEmployee().getId() : null)
                .employeeUsername(b.getEmployee() != null ? b.getEmployee().getUsername() : null)
                .build();
    }
}

