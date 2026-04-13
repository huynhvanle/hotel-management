package com.web.hotel_management.booking.entity;

import com.web.hotel_management.catalog.entity.ServiceProduct;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "UsedService")
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
    @JoinColumn(name = "serviceID")
    private ServiceProduct service;

    private Integer quantity;

    private Double discount;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bookedRoomID")
    private BookedRoom bookedRoom;
}
