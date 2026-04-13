package com.web.hotel_management.client.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Khách hàng — theo ERD: idCardNumber, fullName, address, phone, email, description.
 */
@Entity
@Table(name = "Client")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private Long idCardNumber;

    @Column(nullable = false)
    private String fullName;

    private String address;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @Column(length = 500)
    private String description;
}
