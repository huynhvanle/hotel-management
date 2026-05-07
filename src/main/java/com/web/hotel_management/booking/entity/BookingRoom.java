package com.web.hotel_management.booking.entity;

import com.web.hotel_management.room.entity.Room;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "BookingRoom")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "bookingID")
    private Booking booking;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "roomID")
    private Room room;

    /** Chốt giá tại thời điểm tạo booking (BR2: không hồi tố khi thay đổi giá RoomType). */
    @Column
    private Double unitPrice;
}

