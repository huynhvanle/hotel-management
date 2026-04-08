package com.web.hotel_management.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;
    private Double price;
    private String unit;

    @ManyToOne
    @JoinColumn(name = "hotelID")
    private Hotel hotel;
}