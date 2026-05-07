package com.web.hotel_management.invoice.entity;

import com.web.hotel_management.booking.entity.Booking;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Invoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingID", unique = true)
    private Booking booking;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private Double depositAmount;

    @Column(nullable = false)
    private Double paidAmount;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime paidAt;
}

