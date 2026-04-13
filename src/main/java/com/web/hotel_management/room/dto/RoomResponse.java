package com.web.hotel_management.room.dto;

import com.web.hotel_management.room.entity.Room;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomResponse {
    private String id;
    private String name;
    private String type;
    private Double price;
    private String description;

    private Integer hotelId;
    private String hotelName;

    public static RoomResponse fromEntity(Room r) {
        return RoomResponse.builder()
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
