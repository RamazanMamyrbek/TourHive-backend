package com.tourhive.backend.repositories.organizers;

import com.tourhive.backend.entities.organizers.BusinessProfile;
import com.tourhive.backend.entities.organizers.BusinessVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, UUID> {

    Optional<BusinessProfile> findByUserId(UUID userId);

    Page<BusinessProfile> findAllByStatus(BusinessVerificationStatus status, Pageable pageable);

    boolean existsByUserIdAndStatusIn(UUID userId, Collection<BusinessVerificationStatus> statuses);
}
