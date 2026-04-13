package com.web.hotel_management.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Dịch vụ add-on (minibar, giặt...) — bảng {@code tblService} trong biểu đồ (tên class tránh xung đột với {@code org.springframework.stereotype.Service}).
 */
@Entity
@Table(name = "Service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    private String unit;

    @Column(precision = 19, scale = 2)
    private BigDecimal price;
}
