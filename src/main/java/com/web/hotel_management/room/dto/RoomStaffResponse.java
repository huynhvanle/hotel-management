package com.web.hotel_management.room.dto;

import com.web.hotel_management.room.entity.Room;
import com.web.hotel_management.room.entity.RoomStatus;
import com.web.hotel_management.roomtype.entity.RoomType;
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
    private Integer floor;
    private RoomStatus status;
    private Integer hotelId;
    private String hotelName;
    private Integer roomTypeId;
    private String roomTypeName;
    private Double price;

    public static RoomStaffResponse fromEntity(Room r) {
        if (r == null) return null;
        RoomType rt = r.getRoomType();
        return RoomStaffResponse.builder()
                .id(r.getId())
                .floor(r.getFloor())
                .status(r.getStatus())
                .hotelId(r.getHotel() != null ? r.getHotel().getId() : null)
                .hotelName(r.getHotel() != null ? r.getHotel().getName() : null)
                .roomTypeId(rt != null ? rt.getId() : null)
                .roomTypeName(rt != null ? rt.getName() : null)
                .price(rt != null ? rt.getBasePrice() : null)
                .build();
    }
}

