package com.web.hotel_management.room.dto;

import com.web.hotel_management.room.entity.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomStaffResponse {

    private String id;
    private String name;
    private String type;
    private Double price;
    private String description;
    private Integer hotelId;
    private String hotelName;

    public static RoomStaffResponse fromEntity(Room r) {
        if (r == null) return null;
        return RoomStaffResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .type(r.getType())
                .price(r.getPrice())
                .description(r.getDescription())
                .hotelId(r.getHotel() != null ? r.getHotel().getId() : null)
                .hotelName(r.getHotel() != null ? r.getHotel().getName() : null)
                .build();
    }
}

