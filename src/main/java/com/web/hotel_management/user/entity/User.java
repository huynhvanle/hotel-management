package com.web.hotel_management.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tblUser")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 255)
    private String username;

    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "fullName", nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false, length = 255)
    private String position;

    @Column(name = "mail", length = 255)
    private String mail;

    @Column(length = 255)
    private String description;
}
