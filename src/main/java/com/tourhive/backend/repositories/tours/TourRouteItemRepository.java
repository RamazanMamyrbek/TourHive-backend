package com.tourhive.backend.repositories.tours;

import com.tourhive.backend.entities.tours.TourRouteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TourRouteItemRepository extends JpaRepository<TourRouteItem, UUID> {

    List<TourRouteItem> findAllByTourIdOrderByOrderIndexAsc(UUID tourId);
}
