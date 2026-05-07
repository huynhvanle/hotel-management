package com.web.hotel_management.activity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ActivityLog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // staff actor (User.id). null if actor is a client/anonymous.
    @Column(name = "user_id")
    private Integer userId;

    // client actor (Client.id). null if actor is staff/anonymous.
    @Column(name = "client_id")
    private Integer clientId;

    @Column(nullable = false, length = 50)
    private String actorRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 100)
    private String targetType;

    @Column(length = 100)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}

