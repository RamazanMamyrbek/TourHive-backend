package com.tourhive.backend.entities.tours;

import com.tourhive.backend.entities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "tour_route_items")
public class TourRouteItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "route_date")
    private LocalDate routeDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    public TourRouteItem() {
    }

    @Builder
    public TourRouteItem(Tour tour, String city, LocalDate routeDate, String description, Integer orderIndex) {
        this.tour = tour;
        this.city = city;
        this.routeDate = routeDate;
        this.description = description;
        this.orderIndex = orderIndex != null ? orderIndex : 0;
    }
}
