package com.web.hotel_management.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "used_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsedService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "serviceID")
    private HotelService service;

    @ManyToOne
    @JoinColumn(name = "bookedRoomID")
    private BookedRoom bookedRoom;

    private Integer quantity;
    private Double unitPrice;
    private LocalDateTime usedTime;

    @PrePersist
    protected void onCreate() {
        usedTime = LocalDateTime.now();
    }
}