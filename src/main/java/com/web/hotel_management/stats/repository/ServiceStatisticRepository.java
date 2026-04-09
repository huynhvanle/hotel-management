package com.web.hotel_management.stats.repository;

import com.web.hotel_management.stats.entity.ServiceStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceStatisticRepository extends JpaRepository<ServiceStatistic, Integer> {

    List<ServiceStatistic> findByService_Id(Integer serviceId);
}
