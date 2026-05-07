package com.web.hotel_management.booking.repository;

import com.web.hotel_management.booking.entity.BookingRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRoomRepository extends JpaRepository<BookingRoom, Integer> {

    boolean existsByRoom_Id(String roomId);

    @Query("""
            select count(br) > 0
            from BookingRoom br
            join br.booking b
            where br.room.id = :roomId
              and b.checkin < :checkout
              and b.checkout > :checkin
              and b.status <> 'CANCELLED'
            """)
    boolean existsOverlappingActiveBooking(
            @Param("roomId") String roomId,
            @Param("checkin") LocalDate checkin,
            @Param("checkout") LocalDate checkout
    );

    /** Phòng đang gắn với đơn đã check-in và đang trong thời gian lưu trú. */
    @Query("""
            select count(br) > 0
            from BookingRoom br
            join br.booking b
            where br.room.id = :roomId
              and b.checkin <= :today
              and b.checkout > :today
              and b.status <> 'CANCELLED'
              and b.checkedInAt is not null
            """)
    boolean existsActiveStay(
            @Param("roomId") String roomId,
            @Param("today") LocalDate today
    );

    List<BookingRoom> findByBooking_Id(Integer bookingId);

    @Query("""
            select distinct b.id
            from BookingRoom br
            join br.booking b
            join br.room r
            where r.hotel.id = :hotelId
            """)
    List<Integer> findBookingIdsByHotelId(@Param("hotelId") Integer hotelId);

    @Modifying
    @Query("delete from BookingRoom br where br.booking.id in :bookingIds")
    void deleteByBookingIds(@Param("bookingIds") List<Integer> bookingIds);
}

