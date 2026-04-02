package com.web.hotel_management.entity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "booking")
@Data
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private LocalDateTime bookingDate;
    private Double totalAmount;
    private Integer userId; 

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private List<BookedRoom> bookedRooms;
}