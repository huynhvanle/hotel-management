package com.web.hotel_management.booking.repository;

import com.web.hotel_management.booking.entity.UsedService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsedServiceRepository extends JpaRepository<UsedService, Integer> {

    List<UsedService> findByBookedRoom_Id(Integer bookedRoomId);
}
