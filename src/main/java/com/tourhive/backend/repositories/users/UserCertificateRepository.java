package com.tourhive.backend.repositories.users;

import com.tourhive.backend.entities.users.UserCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserCertificateRepository extends JpaRepository<UserCertificate, UUID> {

    List<UserCertificate> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteAllByUserId(UUID userId);
}

