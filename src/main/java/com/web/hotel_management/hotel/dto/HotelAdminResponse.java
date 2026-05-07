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
    private String address;
    private String description;
    private String phone;
    private String status;

    public static HotelAdminResponse fromEntity(Hotel h) {
        if (h == null) return null;
        return HotelAdminResponse.builder()
                .id(h.getId())
                .name(h.getName())
                .address(h.getAddress())
                .description(h.getDescription())
                .phone(h.getPhone())
                .status(h.getStatus())
                .build();
    }
}

