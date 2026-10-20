package com.tourhive.backend.repositories.tours;

import com.tourhive.backend.entities.tours.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {

    List<ApplicationStatusHistory> findAllByApplicationIdOrderByCreatedAtAsc(UUID applicationId);
}
