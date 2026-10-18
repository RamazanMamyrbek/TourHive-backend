package com.tourhive.backend.repositories.organizers;

import com.tourhive.backend.entities.organizers.BusinessDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BusinessDocumentRepository extends JpaRepository<BusinessDocument, UUID> {

    List<BusinessDocument> findAllByBusinessProfileId(UUID businessProfileId);
}
