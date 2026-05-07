package com.web.hotel_management.user.repository;

import com.web.hotel_management.user.entity.User;
import com.web.hotel_management.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    List<User> findByBranch_IdAndRole(Integer branchId, UserRole role);

    List<User> findByBranch_Id(Integer branchId);

    List<User> findByRole(UserRole role);

    boolean existsByBranch_IdAndRole(Integer branchId, UserRole role);

    @Query("""
            select u from User u
            where u.role = :role
              and u.branch.id = :branchId
              and (:id is null or u.id = :id)
              and (:phone is null or u.phone like concat('%', :phone, '%'))
              and (:fullName is null or lower(u.fullName) like lower(concat('%', :fullName, '%')))
            order by u.id desc
            """)
    List<User> searchByBranchAndRole(
            @Param("branchId") Integer branchId,
            @Param("role") UserRole role,
            @Param("id") Integer id,
            @Param("phone") String phone,
            @Param("fullName") String fullName
    );

    List<User> findTop1ByOrderByIdAsc();
}
