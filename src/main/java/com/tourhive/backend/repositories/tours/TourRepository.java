package com.tourhive.backend.repositories.tours;

import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TourRepository extends JpaRepository<Tour, UUID> {

    List<Tour> findAllByOrganizerIdAndStatusOrderByCreatedAtDesc(UUID organizerId, TourStatus status);

    List<Tour> findAllByOrganizerIdOrderByCreatedAtDesc(UUID organizerId);
}
