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

    @Query("""
            select r from Room r
            where (:hotelId is null or r.hotel.id = :hotelId)
              and (:type is null or lower(r.type) like lower(concat('%', :type, '%')))
              and (:minPrice is null or r.price >= :minPrice)
              and (:maxPrice is null or r.price <= :maxPrice)
            """)
    List<Room> search(
            @Param("hotelId") Integer hotelId,
            @Param("type") String type,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );

    @Query("""
            select r from Room r
            where (:hotelId is null or r.hotel.id = :hotelId)
              and (:type is null or lower(r.type) like lower(concat('%', :type, '%')))
              and (:minPrice is null or r.price >= :minPrice)
              and (:maxPrice is null or r.price <= :maxPrice)
              and not exists (
                select 1 from BookedRoom br
                where br.room = r
                  and br.checkin < :checkout
                  and br.checkout > :checkin
              )
            """)
    List<Room> searchAvailable(
            @Param("hotelId") Integer hotelId,
            @Param("type") String type,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("checkin") LocalDate checkin,
            @Param("checkout") LocalDate checkout
    );
}
