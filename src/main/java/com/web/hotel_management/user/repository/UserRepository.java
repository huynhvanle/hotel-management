package com.web.hotel_management.user.repository;

import com.web.hotel_management.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    Optional<User> findByMail(String mail);

    boolean existsByUsername(String username);

    boolean existsByMail(String mail);
}
