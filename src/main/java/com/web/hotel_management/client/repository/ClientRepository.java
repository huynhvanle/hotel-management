package com.web.hotel_management.client.repository;

import com.web.hotel_management.client.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {
    Optional<Client> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<Client> findByIdCardNumber(Integer idCardNumber);
    boolean existsByIdCardNumber(Integer idCardNumber);
}
