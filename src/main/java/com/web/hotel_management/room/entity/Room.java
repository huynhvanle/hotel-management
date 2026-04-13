package com.web.hotel_management.room.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.web.hotel_management.hotel.entity.Hotel;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @Column(length = 255)
    private String id;

    private String name;

    private String type;

    private Double price;

    @Column(length = 500)
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hotelID")
    @JsonIgnore
    private Hotel hotel;
}
