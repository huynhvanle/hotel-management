package com.web.hotel_management.invoice.repository;

import com.web.hotel_management.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
    Optional<Invoice> findByBooking_Id(Integer bookingId);
    boolean existsByBooking_Id(Integer bookingId);

    @Query("""
            select distinct i from Invoice i
            join i.booking b
            join b.rooms br
            join br.room r
            where r.hotel.id = :branchId
              and i.paidAt >= :from
              and i.paidAt < :to
            """)
    List<Invoice> findPaidInvoicesForBranchInRange(
            @Param("branchId") Integer branchId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("""
            select distinct r.hotel.id, r.hotel.name, i.paidAt, i.paidAmount
            from Invoice i
            join i.booking b
            join b.rooms br
            join br.room r
            where i.paidAt >= :from
              and i.paidAt < :to
            """)
    List<Object[]> findPaidInvoiceRowsAllBranchesInRange(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Modifying
    @Query("delete from Invoice i where i.booking.id in :bookingIds")
    void deleteByBookingIds(@Param("bookingIds") List<Integer> bookingIds);
}

