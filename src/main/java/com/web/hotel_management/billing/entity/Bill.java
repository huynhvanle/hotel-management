package com.web.hotel_management.billing.entity;

import com.web.hotel_management.booking.entity.Booking;
import com.web.hotel_management.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "Bill")
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

    private Double paymentAmount;

    private Integer paymentType;

    @Column(length = 500)
    private String note;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingID")
    private Booking booking;

    /** Lễ tân / nhân viên thu tiền — {@code receptionist} trong biểu đồ. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "userID")
    private User receptionist;
}
