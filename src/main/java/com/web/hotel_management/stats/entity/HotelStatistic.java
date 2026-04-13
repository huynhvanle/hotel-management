package com.web.hotel_management.stats.entity;

import com.web.hotel_management.hotel.entity.Hotel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Thống kê theo khách sạn (mở rộng khái niệm Hotel trong biểu đồ). */
@Entity
@Table(name = "HotelStat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(precision = 19, scale = 4)
    private BigDecimal fillRate;

    @Column(precision = 19, scale = 2)
    private BigDecimal revenue;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", unique = true)
    private Hotel hotel;
}
