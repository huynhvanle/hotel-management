package com.web.hotel_management.booking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.web.hotel_management.room.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "BookedRoom")
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

    private Double discount;

    @Column(nullable = false)
    private Integer isCheckedIn;

    @Column(length = 500)
    private String note;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingID")
    @JsonIgnore
    private Booking booking;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "roomID")
    private Room room;

    @OneToMany(mappedBy = "bookedRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<UsedService> usedServices = new ArrayList<>();
}
