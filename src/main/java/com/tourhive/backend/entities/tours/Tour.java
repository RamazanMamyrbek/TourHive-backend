package com.tourhive.backend.entities.tours;

import com.tourhive.backend.entities.BaseEntity;
import com.tourhive.backend.entities.users.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "tours")
public class Tour extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "tour_type", nullable = false, length = 50)
    private TourType tourType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TourStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "available_places", nullable = false)
    private Integer availablePlaces;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", length = 10)
    private String currency = "KZT";

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private java.time.Instant reviewedAt;

    @Column(name = "published_at")
    private java.time.Instant publishedAt;

    @ElementCollection
    @CollectionTable(name = "tour_countries", joinColumns = @JoinColumn(name = "tour_id"))
    @Column(name = "country", nullable = false, length = 100)
    private List<String> countries = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tour_cities", joinColumns = @JoinColumn(name = "tour_id"))
    @Column(name = "city", nullable = false, length = 100)
    private List<String> cities = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "tour_includes", joinColumns = @JoinColumn(name = "tour_id"))
    @Column(name = "include_item", nullable = false, length = 100)
    private List<String> includes = new ArrayList<>();

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<TourRouteItem> routeItems = new ArrayList<>();

    @OneToMany(mappedBy = "tour", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<TourImage> images = new ArrayList<>();

    public Tour() {
    }

    @Builder
    public Tour(
            User organizer,
            String title,
            String description,
            TourType tourType,
            TourStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Integer capacity,
            Integer availablePlaces,
            BigDecimal price,
            String currency,
            String rejectionReason,
            List<String> countries,
            List<String> cities,
            List<String> includes,
            List<TourRouteItem> routeItems,
            List<TourImage> images
    ) {
        this.organizer = organizer;
        this.title = title;
        this.description = description;
        this.tourType = tourType;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.capacity = capacity;
        this.availablePlaces = availablePlaces != null ? availablePlaces : capacity;
        this.price = price;
        this.currency = currency != null ? currency : "KZT";
        this.rejectionReason = rejectionReason;
        if (countries != null) this.countries = countries;
        if (cities != null) this.cities = cities;
        if (includes != null) this.includes = includes;
        if (routeItems != null) {
            for (TourRouteItem item : routeItems) {
                addRouteItem(item);
            }
        }
        if (images != null) {
            for (TourImage image : images) {
                addImage(image);
            }
        }
    }

    public void addRouteItem(TourRouteItem item) {
        routeItems.add(item);
        item.setTour(this);
    }

    public void removeRouteItem(TourRouteItem item) {
        routeItems.remove(item);
        item.setTour(null);
    }

    public void addImage(TourImage image) {
        images.add(image);
        image.setTour(this);
    }

    public void removeImage(TourImage image) {
        images.remove(image);
        image.setTour(null);
    }

    public String getCoverImageUrl() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsCover()))
                .findFirst()
                .or(() -> images.stream().findFirst())
                .filter(img -> img.getResource() != null)
                .map(img -> "/api/v1/resources/" + img.getResource().getId())
                .orElse(null);
    }
}
