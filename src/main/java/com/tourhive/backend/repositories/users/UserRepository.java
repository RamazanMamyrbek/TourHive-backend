package com.tourhive.backend.repositories.users;

import com.tourhive.backend.entities.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @org.springframework.data.jpa.repository.Query("""
        SELECT u FROM User u
        WHERE (:role IS NULL OR u.role = :role)
          AND (:status IS NULL OR u.status = :status)
    """)
    org.springframework.data.domain.Page<User> findUsersFilteredWithoutSearch(
            @org.springframework.data.repository.query.Param("role") com.tourhive.backend.entities.users.Role role,
            @org.springframework.data.repository.query.Param("status") com.tourhive.backend.entities.users.UserStatus status,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("""
        SELECT u FROM User u
        WHERE (:role IS NULL OR u.role = :role)
          AND (:status IS NULL OR u.status = :status)
          AND LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    org.springframework.data.domain.Page<User> findUsersFilteredWithSearch(
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("role") com.tourhive.backend.entities.users.Role role,
            @org.springframework.data.repository.query.Param("status") com.tourhive.backend.entities.users.UserStatus status,
            org.springframework.data.domain.Pageable pageable
    );
}
