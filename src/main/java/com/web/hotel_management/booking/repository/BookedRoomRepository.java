package com.web.hotel_management.booking.repository;

import com.web.hotel_management.booking.entity.BookedRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookedRoomRepository extends JpaRepository<BookedRoom, Integer> {

    List<BookedRoom> findByBooking_Id(Integer bookingId);

    List<BookedRoom> findByRoom_Id(String roomId);

    /**
     * Overlap condition: existing.checkin < checkout AND existing.checkout > checkin
     */
    @Query("""
            select count(br) > 0 from BookedRoom br
            where br.room.id = :roomId
              and br.checkin < :checkout
              and br.checkout > :checkin
            """)
    boolean existsOverlappingBooking(
            @Param("roomId") String roomId,
            @Param("checkin") LocalDate checkin,
            @Param("checkout") LocalDate checkout
    );
}
