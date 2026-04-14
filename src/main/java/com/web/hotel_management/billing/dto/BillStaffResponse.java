package com.web.hotel_management.billing.dto;

import com.web.hotel_management.billing.entity.Bill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillStaffResponse {

    private Integer id;
    private LocalDate paymentDate;
    private Double paymentAmount;
    private Integer paymentType;
    private String note;

    private Integer bookingId;
    private String clientName;
    private String clientEmail;

    private Integer receptionistId;
    private String receptionistUsername;

    public static BillStaffResponse fromEntity(Bill b) {
        if (b == null) return null;
        return BillStaffResponse.builder()
                .id(b.getId())
                .paymentDate(b.getPaymentDate())
                .paymentAmount(b.getPaymentAmount())
                .paymentType(b.getPaymentType())
                .note(b.getNote())
                .bookingId(b.getBooking() != null ? b.getBooking().getId() : null)
                .clientName(b.getBooking() != null && b.getBooking().getClient() != null ? b.getBooking().getClient().getFullName() : null)
                .clientEmail(b.getBooking() != null && b.getBooking().getClient() != null ? b.getBooking().getClient().getEmail() : null)
                .receptionistId(b.getReceptionist() != null ? b.getReceptionist().getId() : null)
                .receptionistUsername(b.getReceptionist() != null ? b.getReceptionist().getUsername() : null)
                .build();
    }
}

