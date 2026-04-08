package com.web.hotel_management.stats.repository;

import com.web.hotel_management.stats.entity.ClientStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientStatisticRepository extends JpaRepository<ClientStatistic, Integer> {

    List<ClientStatistic> findByClient_Id(Integer clientId);
}
