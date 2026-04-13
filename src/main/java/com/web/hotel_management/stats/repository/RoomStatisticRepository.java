package com.web.hotel_management.stats.repository;

import com.web.hotel_management.stats.entity.RoomStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomStatisticRepository extends JpaRepository<RoomStatistic, Integer> {

    List<RoomStatistic> findByRoom_Id(String roomId);
}
