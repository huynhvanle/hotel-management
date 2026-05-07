package com.web.hotel_management.booking.repository;

import com.web.hotel_management.booking.entity.Booking;
import com.web.hotel_management.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findByClient_Id(Integer clientId);

    List<Booking> findByClient_PhoneContainingIgnoreCase(String phone);

    @Query("""
            select b from Booking b
            join b.client c
            where (:bookingId is null or b.id = :bookingId)
              and (:phone is null or :phone = '' or c.phone like concat('%', :phone, '%'))
            order by b.id desc
            """)
    List<Booking> searchByIdAndPhone(@Param("bookingId") Integer bookingId, @Param("phone") String phone);

    @Query("""
            select b from Booking b
            where b.status = :status
              and b.createdAt is not null
              and b.createdAt < :cutoff
            """)
    List<Booking> findPendingCreatedBefore(@Param("status") BookingStatus status, @Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("delete from Booking b where b.id in :ids")
    void deleteByIds(@Param("ids") List<Integer> ids);
}
