package com.web.hotel_management.reception.dto;

import com.web.hotel_management.room.entity.RoomStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReceptionRoomItemResponse {
    /** Số phòng dạng số (vd: 301). */
    private String roomNumber;
    /** Id gốc trong DB (vd: R-301 nếu có). */
    private String rawId;
    /** Tầng suy ra từ chữ số đầu tiên của roomNumber. */
    private Integer floor;
    private RoomStatus status;
    private String roomTypeName;
    private Double price;
    private Integer hotelId;
    private String hotelName;
}

