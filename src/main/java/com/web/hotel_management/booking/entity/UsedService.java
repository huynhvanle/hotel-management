package com.web.hotel_management.booking.entity;

import com.web.hotel_management.catalog.entity.ServiceProduct;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tblUsedService")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsedService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceProduct service;

    private Integer quantity;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booked_room_id")
    private BookedRoom bookedRoom;
}
