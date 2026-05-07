package com.web.hotel_management.client.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Khách hàng (vừa profile vừa account đăng nhập).
 */
@Entity
@Table(
        name = "Client",
        uniqueConstraints = {@UniqueConstraint(name = "uk_client_phone", columnNames = "phone")})
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

    /** Đăng nhập / định danh JWT — mỗi số chỉ một tài khoản khách (ràng buộc uk_client_phone). */
    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    private String address;
}
