package com.web.hotel_management.booking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.web.hotel_management.client.entity.Client;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Thời điểm tạo đơn (dùng để tự huỷ PENDING quá hạn). */
    private LocalDateTime createdAt;

    private LocalDate checkin;

    private LocalDate checkout;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    private Double depositAmount;

    /** Thời điểm check-in (không dùng Folio lưu DB). */
    private LocalDateTime checkedInAt;

    /** Thời điểm check-out (sau khi đã xuất hoá đơn và xác nhận thanh toán). */
    private LocalDateTime checkedOutAt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "clientID")
    private Client client;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<BookingRoom> rooms = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
