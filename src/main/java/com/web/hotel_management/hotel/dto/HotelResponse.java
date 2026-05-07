package com.web.hotel_management.hotel.dto;

import com.web.hotel_management.hotel.entity.Hotel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HotelResponse {
    private Integer id;
    private String name;
    private String address;
    private String description;
    private String phone;
    private String status;

    public static HotelResponse fromEntity(Hotel h) {
        return HotelResponse.builder()
                .id(h.getId())
                .name(h.getName())
                .address(h.getAddress())
                .description(h.getDescription())
                .phone(h.getPhone())
                .status(h.getStatus())
                .build();
    }
}
