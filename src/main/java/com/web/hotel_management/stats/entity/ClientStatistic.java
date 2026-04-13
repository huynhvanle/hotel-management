package com.web.hotel_management.stats.entity;

import com.web.hotel_management.client.entity.Client;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ClientStat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer day;

    @Column(precision = 19, scale = 2)
    private BigDecimal payment;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;
}
