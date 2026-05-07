package com.web.hotel_management.client.repository;

import com.web.hotel_management.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {

    Optional<Client> findByPhone(String phone);

    boolean existsByPhone(String phone);

    Optional<Client> findByIdCardNumber(Long idCardNumber);

    boolean existsByIdCardNumber(Long idCardNumber);

    @Query("""
            select c from Client c
            where (:idCardNumber is null or c.idCardNumber = :idCardNumber)
              and (:phone is null or :phone = '' or c.phone like concat('%', :phone, '%'))
            order by c.id desc
            """)
    List<Client> searchByIdCardOrPhone(@Param("idCardNumber") Long idCardNumber, @Param("phone") String phone);
}
