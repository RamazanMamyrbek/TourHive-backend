package com.tourhive.backend.repositories.tours;

import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID>, JpaSpecificationExecutor<Tour> {

    List<Tour> findAllByOrganizerIdAndStatusOrderByCreatedAtDesc(UUID organizerId, TourStatus status);

    List<Tour> findAllByOrganizerIdOrderByCreatedAtDesc(UUID organizerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tour t WHERE t.id = :id")
    Optional<Tour> findByIdWithPessimisticLock(@Param("id") UUID id);

    long countByOrganizerId(UUID organizerId);

    boolean existsByOrganizerIdAndStatusAndEndDateGreaterThanEqual(UUID organizerId, TourStatus status, java.time.LocalDate date);
}
