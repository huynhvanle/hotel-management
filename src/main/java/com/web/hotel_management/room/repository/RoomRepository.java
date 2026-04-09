package com.web.hotel_management.room.repository;

import com.web.hotel_management.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {

    List<Room> findByHotel_Id(Integer hotelId);
}
