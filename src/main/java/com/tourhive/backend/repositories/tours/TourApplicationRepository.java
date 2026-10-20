package com.tourhive.backend.repositories.tours;

import com.tourhive.backend.entities.tours.TourApplication;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TourApplicationRepository extends JpaRepository<TourApplication, UUID> {

    boolean existsByTourIdAndApplicantIdAndStatusIn(UUID tourId, UUID applicantId, Collection<TourApplicationStatus> statuses);

    Optional<TourApplication> findByTourIdAndApplicantIdAndStatusIn(UUID tourId, UUID applicantId, Collection<TourApplicationStatus> statuses);

    Page<TourApplication> findAllByTourId(UUID tourId, Pageable pageable);

    Page<TourApplication> findAllByTourIdAndStatus(UUID tourId, TourApplicationStatus status, Pageable pageable);

    Page<TourApplication> findAllByApplicantId(UUID applicantId, Pageable pageable);

    Page<TourApplication> findAllByApplicantIdAndStatus(UUID applicantId, TourApplicationStatus status, Pageable pageable);

    boolean existsByTourIdAndApplicantIdAndStatus(UUID tourId, UUID applicantId, TourApplicationStatus status);

    List<TourApplication> findAllByTourIdAndStatusIn(UUID tourId, Collection<TourApplicationStatus> statuses);

    @org.springframework.data.jpa.repository.Query("""
        SELECT CASE WHEN count(a) > 0 THEN true ELSE false END
        FROM TourApplication a
        WHERE (a.applicant.id = :u1 AND a.tour.organizer.id = :u2)
           OR (a.applicant.id = :u2 AND a.tour.organizer.id = :u1)
    """)
    boolean hasTourRelationshipBetween(
            @org.springframework.data.repository.query.Param("u1") UUID u1,
            @org.springframework.data.repository.query.Param("u2") UUID u2
    );

    long countByApplicantId(UUID applicantId);

    List<TourApplication> findAllByApplicantIdAndStatusIn(UUID applicantId, Collection<TourApplicationStatus> statuses);
}
