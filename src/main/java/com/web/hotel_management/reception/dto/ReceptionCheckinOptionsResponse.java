package com.web.hotel_management.reception.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionCheckinOptionsResponse {
    private Integer bookingId;
    private List<RoomTypeOption> roomTypes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomTypeOption {
        private Integer roomTypeId;
        private String roomTypeName;
        private Integer quantity;
        /** Danh sách phòng vật lí AVAILABLE phù hợp để check-in (theo loại phòng). */
        private List<String> availableRoomIds;
    }
}

