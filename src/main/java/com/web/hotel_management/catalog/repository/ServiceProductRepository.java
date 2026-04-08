package com.web.hotel_management.catalog.repository;

import com.web.hotel_management.catalog.entity.ServiceProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceProductRepository extends JpaRepository<ServiceProduct, Integer> {
}
