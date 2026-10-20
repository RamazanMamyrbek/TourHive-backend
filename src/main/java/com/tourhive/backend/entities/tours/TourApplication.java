package com.tourhive.backend.entities.tours;

import com.tourhive.backend.entities.BaseEntity;
import com.tourhive.backend.entities.users.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tour_applications")
public class TourApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TourApplicationStatus status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "contact_info", length = 255)
    private String contactInfo;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Builder
    public TourApplication(
            Tour tour,
            User applicant,
            TourApplicationStatus status,
            String message,
            String contactInfo,
            String rejectionReason,
            String cancellationReason
    ) {
        this.tour = tour;
        this.applicant = applicant;
        this.status = status != null ? status : TourApplicationStatus.PENDING;
        this.message = message;
        this.contactInfo = contactInfo;
        this.rejectionReason = rejectionReason;
        this.cancellationReason = cancellationReason;
    }
}
