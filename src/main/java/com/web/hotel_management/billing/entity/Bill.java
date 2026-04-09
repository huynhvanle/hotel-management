package com.web.hotel_management.billing.entity;

import com.web.hotel_management.booking.entity.Booking;
import com.web.hotel_management.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tblBill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDate paymentDate;

    @Column(precision = 19, scale = 2)
    private BigDecimal paymentAmount;

    @Column(length = 50)
    private String paymentType;

    @Column(length = 500)
    private String note;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    /** Lễ tân / nhân viên thu tiền — {@code receptionist} trong biểu đồ. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "receptionist_id")
    private User receptionist;
}
