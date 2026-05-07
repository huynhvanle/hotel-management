package com.web.hotel_management.reception.dto;

import com.web.hotel_management.booking.entity.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ReceptionBookingDetailResponse {
    private Integer bookingId;
    private String clientPhone;
    private String clientFullName;
    private LocalDate checkin;
    private LocalDate checkout;
    private BookingStatus status;
    private Double depositAmount;
    /** true nếu đã check-in, lúc đó mới hiển thị phòng vật lí. */
    private Boolean checkedIn;
    /** true nếu đã lưu hoá đơn (đã thanh toán đủ). */
    private Boolean invoiceSaved;
    /** true nếu đã check-out. */
    private Boolean checkedOut;
    /** Hạng phòng + số lượng (hiển thị trước check-in). */
    private List<RoomTypeLine> roomTypes;
    /** Danh sách phòng vật lí (chỉ trả về sau check-in). */
    private List<String> roomIds;

    @Data
    @Builder
    public static class RoomTypeLine {
        private Integer roomTypeId;
        private String roomTypeName;
        private Integer quantity;
    }
}

