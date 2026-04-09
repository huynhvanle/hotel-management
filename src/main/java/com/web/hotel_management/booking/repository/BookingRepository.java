package com.web.hotel_management.booking.repository;

import com.web.hotel_management.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    List<Booking> findByClient_Id(Integer clientId);

    List<Booking> findByEmployee_Id(Integer employeeId);
}
