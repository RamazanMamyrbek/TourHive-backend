package com.tourhive.backend.repositories.favorites;

import com.tourhive.backend.entities.favorites.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    Optional<Favorite> findByUserIdAndTourId(UUID userId, UUID tourId);

    boolean existsByUserIdAndTourId(UUID userId, UUID tourId);

    Page<Favorite> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.user.id = :userId AND f.tour.id = :tourId")
    int deleteByUserIdAndTourId(@Param("userId") UUID userId, @Param("tourId") UUID tourId);
}
