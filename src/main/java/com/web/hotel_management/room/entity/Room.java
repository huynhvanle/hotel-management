package com.web.hotel_management.room.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.web.hotel_management.hotel.entity.Hotel;
import com.web.hotel_management.roomtype.entity.RoomType;
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

    /** Tầng */
    @Column(nullable = false)
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RoomStatus status = RoomStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomTypeId")
    private RoomType roomType;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hotelID")
    @JsonIgnore
    private Hotel hotel;
}
