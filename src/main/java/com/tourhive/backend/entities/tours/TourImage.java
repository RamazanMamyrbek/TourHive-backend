package com.tourhive.backend.entities.tours;

import com.tourhive.backend.entities.BaseEntity;
import com.tourhive.backend.entities.resources.Resource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tour_images")
public class TourImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "is_cover", nullable = false)
    private Boolean isCover = false;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex = 0;

    public TourImage() {
    }

    @Builder
    public TourImage(Tour tour, Resource resource, Boolean isCover, Integer orderIndex) {
        this.tour = tour;
        this.resource = resource;
        this.isCover = isCover != null ? isCover : false;
        this.orderIndex = orderIndex != null ? orderIndex : 0;
    }
}
