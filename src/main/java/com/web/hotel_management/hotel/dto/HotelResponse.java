package com.web.hotel_management.hotel.dto;

import com.web.hotel_management.hotel.entity.Hotel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HotelResponse {
    private Integer id;
    private String name;
    private Integer starLevel;
    private String address;
    private String description;

    public static HotelResponse fromEntity(Hotel h) {
        return HotelResponse.builder()
                .id(h.getId())
                .name(h.getName())
                .starLevel(h.getStarLevel())
                .address(h.getAddress())
                .description(h.getDescription())
                .build();
    }
}
