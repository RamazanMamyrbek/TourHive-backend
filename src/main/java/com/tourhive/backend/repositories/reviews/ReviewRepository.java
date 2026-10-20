package com.tourhive.backend.repositories.reviews;

import com.tourhive.backend.entities.reviews.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findAllByTourIdOrderByCreatedAtDesc(UUID tourId, Pageable pageable);

    Page<Review> findAllByOrganizerIdOrderByCreatedAtDesc(UUID organizerId, Pageable pageable);

    Page<Review> findAllByAuthorIdOrderByCreatedAtDesc(UUID authorId, Pageable pageable);

    boolean existsByTourIdAndAuthorId(UUID tourId, UUID authorId);

    Optional<Review> findByTourIdAndAuthorId(UUID tourId, UUID authorId);

    @Query("SELECT coalesce(avg(r.rating), 0.0) FROM Review r WHERE r.organizer.id = :organizerId")
    Double calculateAverageRatingForOrganizer(@Param("organizerId") UUID organizerId);

    @Query("SELECT count(r) FROM Review r WHERE r.organizer.id = :organizerId")
    int countReviewsForOrganizer(@Param("organizerId") UUID organizerId);

    @Query("SELECT coalesce(avg(r.rating), 0.0) FROM Review r WHERE r.tour.id = :tourId")
    Double calculateAverageRatingForTour(@Param("tourId") UUID tourId);

    @Query("SELECT count(r) FROM Review r WHERE r.tour.id = :tourId")
    int countReviewsForTour(@Param("tourId") UUID tourId);
}
