package com.web.hotel_management.stats.repository;

import com.web.hotel_management.stats.entity.IncomeStatistic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomeStatisticRepository extends JpaRepository<IncomeStatistic, Integer> {
}
