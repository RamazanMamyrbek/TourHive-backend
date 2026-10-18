package com.tourhive.backend.entities.organizers;

import com.tourhive.backend.entities.BaseEntity;
import com.tourhive.backend.entities.users.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "business_profiles")
public class BusinessProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "registration_number", nullable = false, length = 100)
    private String registrationNumber;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone", nullable = false, length = 50)
    private String contactPhone;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private BusinessVerificationStatus status = BusinessVerificationStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @OneToMany(mappedBy = "businessProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BusinessDocument> documents = new ArrayList<>();

    public BusinessProfile() {
    }

    @Builder
    public BusinessProfile(
            User user,
            String legalName,
            String registrationNumber,
            String country,
            String contactEmail,
            String contactPhone,
            String description,
            BusinessVerificationStatus status,
            String rejectionReason,
            User reviewedBy,
            Instant reviewedAt,
            List<BusinessDocument> documents
    ) {
        this.user = user;
        this.legalName = legalName;
        this.registrationNumber = registrationNumber;
        this.country = country;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.description = description;
        this.status = status != null ? status : BusinessVerificationStatus.PENDING;
        this.rejectionReason = rejectionReason;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
        this.documents = documents != null ? documents : new ArrayList<>();
    }

    public void addDocument(BusinessDocument document) {
        documents.add(document);
        document.setBusinessProfile(this);
    }
}
