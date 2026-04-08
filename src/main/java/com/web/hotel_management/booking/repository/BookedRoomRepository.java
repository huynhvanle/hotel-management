package com.web.hotel_management.booking.repository;

import com.web.hotel_management.booking.entity.BookedRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookedRoomRepository extends JpaRepository<BookedRoom, Integer> {

    List<BookedRoom> findByBooking_Id(Integer bookingId);

    List<BookedRoom> findByRoom_Id(Integer roomId);
}
