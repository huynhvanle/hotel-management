package com.web.hotel_management.stats.repository;

import com.web.hotel_management.stats.entity.HotelStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HotelStatisticRepository extends JpaRepository<HotelStatistic, Integer> {

    Optional<HotelStatistic> findByHotel_Id(Integer hotelId);
}
