package com.tourhive.backend.repositories.auth;

import com.tourhive.backend.entities.auth.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    Optional<EmailVerification> findTopByUserIdAndCodeAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId, String code);

    Optional<EmailVerification> findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId);
}
