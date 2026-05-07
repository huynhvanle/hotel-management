package com.web.hotel_management.room.repository;

import com.web.hotel_management.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    List<Room> findByHotel_Id(Integer hotelId);

    void deleteByHotel_Id(Integer hotelId);

    @Query("""
            select r from Room r
            left join r.roomType rt
            where (:hotelId is null or r.hotel.id = :hotelId)
              and (:roomTypeId is null or rt.id = :roomTypeId)
              and (:type is null or lower(rt.name) like lower(concat('%', :type, '%')))
              and (:minPrice is null or coalesce(rt.basePrice, 0) >= :minPrice)
              and (:maxPrice is null or coalesce(rt.basePrice, 0) <= :maxPrice)
            """)
    List<Room> search(
            @Param("hotelId") Integer hotelId,
            @Param("roomTypeId") Integer roomTypeId,
            @Param("type") String type,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );

    @Query("""
            select r from Room r
            left join r.roomType rt
            where (:hotelId is null or r.hotel.id = :hotelId)
              and (:roomTypeId is null or rt.id = :roomTypeId)
              and (:type is null or lower(rt.name) like lower(concat('%', :type, '%')))
              and (:minPrice is null or coalesce(rt.basePrice, 0) >= :minPrice)
              and (:maxPrice is null or coalesce(rt.basePrice, 0) <= :maxPrice)
              and r.status = 'AVAILABLE'
              and not exists (
                select 1 from BookingRoom br
                join br.booking b
                where br.room = r
                  and b.checkin < :checkout
                  and b.checkout > :checkin
                  and b.status <> 'CANCELLED'
              )
            order by rt.id asc, r.id asc
            """)
    List<Room> searchAvailable(
            @Param("hotelId") Integer hotelId,
            @Param("roomTypeId") Integer roomTypeId,
            @Param("type") String type,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("checkin") LocalDate checkin,
            @Param("checkout") LocalDate checkout
    );

    /** Same overlap rules as {@link #searchAvailable}, but only rooms currently marked AVAILABLE (đặt phòng tại quầy). */
    @Query("""
            select r from Room r
            left join r.roomType rt
            where r.hotel.id = :hotelId
              and (:roomTypeId is null or rt.id = :roomTypeId)
              and (:type is null or lower(rt.name) like lower(concat('%', :type, '%')))
              and (:minPrice is null or coalesce(rt.basePrice, 0) >= :minPrice)
              and (:maxPrice is null or coalesce(rt.basePrice, 0) <= :maxPrice)
              and r.status = 'AVAILABLE'
              and not exists (
                select 1 from BookingRoom br
                join br.booking b
                where br.room = r
                  and b.checkin < :checkout
                  and b.checkout > :checkin
                  and b.status <> 'CANCELLED'
              )
            order by rt.id asc, r.id asc
            """)
    List<Room> searchWalkInAvailable(
            @Param("hotelId") Integer hotelId,
            @Param("roomTypeId") Integer roomTypeId,
            @Param("type") String type,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("checkin") LocalDate checkin,
            @Param("checkout") LocalDate checkout
    );

    /** Available rooms for check-in: exclude overlap bookings except the target booking itself. */
    @Query("""
            select r from Room r
            left join r.roomType rt
            where r.hotel.id = :hotelId
              and rt.id = :roomTypeId
              and r.status = 'AVAILABLE'
              and not exists (
                select 1 from BookingRoom br
                join br.booking b
                where br.room = r
                  and b.id <> :excludeBookingId
                  and b.checkin < :checkout
                  and b.checkout > :checkin
                  and b.status <> 'CANCELLED'
              )
            order by r.id asc
            """)
    List<Room> searchAvailableForCheckin(
            @Param("hotelId") Integer hotelId,
            @Param("roomTypeId") Integer roomTypeId,
            @Param("checkin") LocalDate checkin,
            @Param("checkout") LocalDate checkout,
            @Param("excludeBookingId") Integer excludeBookingId
    );
}
