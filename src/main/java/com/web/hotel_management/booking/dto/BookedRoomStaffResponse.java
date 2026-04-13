package com.web.hotel_management.booking.dto;

import com.web.hotel_management.booking.entity.BookedRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookedRoomStaffResponse {

    private Integer id;
    private String roomId;
    private String roomName;
    private String roomType;
    private Double roomPrice;
    private LocalDate checkin;
    private LocalDate checkout;
    private Integer isCheckedIn;
    private String note;

    public static BookedRoomStaffResponse fromEntity(BookedRoom br) {
        if (br == null) return null;
        return BookedRoomStaffResponse.builder()
                .id(br.getId())
                .roomId(br.getRoom() != null ? br.getRoom().getId() : null)
                .roomName(br.getRoom() != null ? br.getRoom().getName() : null)
                .roomType(br.getRoom() != null ? br.getRoom().getType() : null)
                .roomPrice(br.getRoom() != null ? br.getRoom().getPrice() : null)
                .checkin(br.getCheckin())
                .checkout(br.getCheckout())
                .isCheckedIn(br.getIsCheckedIn())
                .note(br.getNote())
                .build();
    }
}

