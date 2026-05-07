package com.web.hotel_management.room.dto;

import com.web.hotel_management.room.entity.Room;
import com.web.hotel_management.room.entity.RoomStatus;
import com.web.hotel_management.roomtype.entity.RoomType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomResponse {
    private String id;
    private Integer floor;
    private RoomStatus status;
    private Integer roomTypeId;
    private String roomTypeName;
    /** Mô tả marketing / giới thiệu theo loại phòng (RoomType.description). */
    private String roomTypeDescription;
    private Double price;

    private Integer hotelId;
    private String hotelName;

    public static RoomResponse fromEntity(Room r) {
        RoomType rt = r.getRoomType();
        return RoomResponse.builder()
                .id(r.getId())
                .floor(r.getFloor())
                .status(r.getStatus())
                .roomTypeId(rt != null ? rt.getId() : null)
                .roomTypeName(rt != null ? rt.getName() : null)
                .roomTypeDescription(rt != null ? rt.getDescription() : null)
                .price(rt != null ? rt.getBasePrice() : null)
                .hotelId(r.getHotel() != null ? r.getHotel().getId() : null)
                .hotelName(r.getHotel() != null ? r.getHotel().getName() : null)
                .build();
    }
}
