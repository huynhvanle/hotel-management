package com.web.hotel_management.billing.repository;

import com.web.hotel_management.billing.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Integer> {

    Optional<Bill> findByBooking_Id(Integer bookingId);

    List<Bill> findByReceptionist_Id(Integer receptionistId);
}
