package com.web.hotel_management.roomtype.dto;

import com.web.hotel_management.roomtype.entity.RoomType;
import com.web.hotel_management.roomtype.entity.RoomTypeStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomTypeDto {
    private Integer id;
    private String code;
    private String name;
    private String description;
    private Double basePrice;
    private RoomTypeStatus status;

    public static RoomTypeDto fromEntity(RoomType rt) {
        if (rt == null) return null;
        return RoomTypeDto.builder()
                .id(rt.getId())
                .code(rt.getCode())
                .name(rt.getName())
                .description(rt.getDescription())
                .basePrice(rt.getBasePrice())
                .status(rt.getStatus())
                .build();
    }
}

