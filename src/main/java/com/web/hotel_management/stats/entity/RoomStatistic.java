package com.web.hotel_management.stats.entity;

import com.web.hotel_management.room.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tblRoomStat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomStatistic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer fillDay;

    @Column(precision = 19, scale = 4)
    private BigDecimal fillRate;

    @Column(precision = 19, scale = 2)
    private BigDecimal revenue;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;
}
