package com.web.hotel_management.repository;

import com.web.hotel_management.entity.HotelService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HotelServiceRepository extends JpaRepository<HotelService, Integer> {
    List<HotelService> findByHotelId(Integer hotelId);
}