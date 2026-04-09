package com.web.hotel_management.booking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.web.hotel_management.room.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tblBookedRoom")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookedRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDate checkin;

    private LocalDate checkout;

    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Boolean isCheckedIn;

    @Column(length = 500)
    private String note;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    @JsonIgnore
    private Booking booking;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @OneToMany(mappedBy = "bookedRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<UsedService> usedServices = new ArrayList<>();
}
