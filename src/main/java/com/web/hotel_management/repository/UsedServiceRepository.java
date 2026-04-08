package com.web.hotel_management.repository;

import com.web.hotel_management.entity.UsedService;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsedServiceRepository extends JpaRepository<UsedService, Integer> {
}