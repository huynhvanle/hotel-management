package com.web.hotel_management.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "bookedroom")
@Data
public class BookedRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private LocalDate checkin;
    private LocalDate checkout;
    private Double price;

    @ManyToOne
    @JoinColumn(name = "bookingID")
    @JsonIgnore
    private Booking booking;

    @ManyToOne
    @JoinColumn(name = "roomID")
    private Room room;

    @OneToMany(mappedBy = "bookedRoom", cascade = CascadeType.ALL)
    private List<UsedService> usedServices;
}