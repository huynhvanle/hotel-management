package com.web.hotel_management.room.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.web.hotel_management.hotel.entity.Hotel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tblRoom")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String type;

    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    @JsonIgnore
    private Hotel hotel;
}
