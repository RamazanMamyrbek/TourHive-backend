package com.tourhive.backend.repositories.tours;

import com.tourhive.backend.entities.tours.TourImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourImageRepository extends JpaRepository<TourImage, UUID> {

    List<TourImage> findAllByTourIdOrderByOrderIndexAsc(UUID tourId);

    Optional<TourImage> findByTourIdAndIsCoverTrue(UUID tourId);
}
