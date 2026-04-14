package com.web.hotel_management.hotel.dto;

import com.web.hotel_management.hotel.entity.Hotel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelAdminResponse {

    private Integer id;
    private String name;
    private Integer starLevel;
    private String address;
    private String description;

    public static HotelAdminResponse fromEntity(Hotel h) {
        if (h == null) return null;
        return HotelAdminResponse.builder()
                .id(h.getId())
                .name(h.getName())
                .starLevel(h.getStarLevel())
                .address(h.getAddress())
                .description(h.getDescription())
                .build();
    }
}

