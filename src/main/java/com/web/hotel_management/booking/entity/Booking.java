package com.web.hotel_management.booking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.web.hotel_management.client.entity.Client;
import com.web.hotel_management.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tblBooking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDate bookingDate;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(length = 500)
    private String note;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    /** Nhân viên xử lý đặt phòng — {@code employee} trong biểu đồ. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private User employee;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<BookedRoom> bookedRooms = new ArrayList<>();
}
