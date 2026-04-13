package com.web.hotel_management.stats.entity;

import com.web.hotel_management.billing.entity.Bill;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "IncomeStat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncomeStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Số khách / chỉ số theo biểu đồ {@code client (int)}. */
    private Integer clientCount;

    @Column(precision = 19, scale = 2)
    private BigDecimal revenue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private Bill bill;
}
