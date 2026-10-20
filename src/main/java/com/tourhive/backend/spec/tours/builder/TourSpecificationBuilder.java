package com.tourhive.backend.spec.tours.builder;

import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.spec.tours.TourSpecification;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class TourSpecificationBuilder {

    private Specification<Tour> spec = Specification.where(null);

    public TourSpecificationBuilder withSearch(String search) {
        var s = TourSpecification.search(search);
        if (s != null) spec = spec.and(s);
        return this;
    }

    public TourSpecificationBuilder withCountry(String country) {
        var s = TourSpecification.hasCountry(country);
        if (s != null) spec = spec.and(s);
        return this;
    }

    public TourSpecificationBuilder withCity(String city) {
        var s = TourSpecification.hasCity(city);
        if (s != null) spec = spec.and(s);
        return this;
    }

    public TourSpecificationBuilder withTourType(TourType tourType) {
        var s = TourSpecification.hasTourType(tourType);
        if (s != null) spec = spec.and(s);
        return this;
    }

    public TourSpecificationBuilder withDateFrom(LocalDate dateFrom) {
        var s = TourSpecification.startDateFrom(dateFrom);
        if (s != null) spec = spec.and(s);
        return this;
    }

    public TourSpecificationBuilder withDateTo(LocalDate dateTo) {
        var s = TourSpecification.startDateTo(dateTo);
        if (s != null) spec = spec.and(s);
        return this;
    }

    public TourSpecificationBuilder withMinPrice(BigDecimal minPrice) {
        var s = TourSpecification.priceGte(minPrice);
        if (s != null) spec = spec.and(s);
        return this;
    }

    public TourSpecificationBuilder withMaxPrice(BigDecimal maxPrice) {
        var s = TourSpecification.priceLte(maxPrice);
        if (s != null) spec = spec.and(s);
        return this;
    }

    public TourSpecificationBuilder withHasAvailablePlaces(Boolean hasAvailablePlaces) {
        var s = TourSpecification.hasAvailablePlaces(hasAvailablePlaces);
        if (s != null) spec = spec.and(s);
        return this;
    }

    public TourSpecificationBuilder withStatus(TourStatus status) {
        var s = TourSpecification.hasStatus(status);
        if (s != null) spec = spec.and(s);
        return this;
    }

    public TourSpecificationBuilder withOrganizerId(UUID organizerId) {
        var s = TourSpecification.hasOrganizerId(organizerId);
        if (s != null) spec = spec.and(s);
        return this;
    }

    public Specification<Tour> build() {
        return spec;
    }
}
