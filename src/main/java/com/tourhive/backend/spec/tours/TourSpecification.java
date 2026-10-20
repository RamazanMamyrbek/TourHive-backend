package com.tourhive.backend.spec.tours;

import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public final class TourSpecification {

    private TourSpecification() {
    }

    public static Specification<Tour> search(String search) {
        if (search == null || search.isBlank()) return null;
        String sanitized = search.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
                .toLowerCase(Locale.ROOT);
        String like = "%" + sanitized + "%";
        return (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("title")), like, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.get("description"), "")), like, '\\')
                );
    }

    public static Specification<Tour> hasCountry(String country) {
        if (country == null || country.isBlank()) return null;
        String normalized = country.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<Tour, String> join = root.join("countries");
            return cb.equal(cb.lower(join), normalized);
        };
    }

    public static Specification<Tour> hasCity(String city) {
        if (city == null || city.isBlank()) return null;
        String normalized = city.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<Tour, String> join = root.join("cities");
            return cb.equal(cb.lower(join), normalized);
        };
    }

    public static Specification<Tour> hasTourType(TourType tourType) {
        if (tourType == null) return null;
        return (root, query, cb) -> cb.equal(root.get("tourType"), tourType);
    }

    public static Specification<Tour> startDateFrom(LocalDate dateFrom) {
        if (dateFrom == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), dateFrom);
    }

    public static Specification<Tour> startDateTo(LocalDate dateTo) {
        if (dateTo == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), dateTo);
    }

    public static Specification<Tour> priceGte(BigDecimal minPrice) {
        if (minPrice == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Tour> priceLte(BigDecimal maxPrice) {
        if (maxPrice == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    public static Specification<Tour> hasAvailablePlaces(Boolean hasAvailablePlaces) {
        if (hasAvailablePlaces == null || !hasAvailablePlaces) return null;
        return (root, query, cb) -> cb.greaterThan(root.get("availablePlaces"), 0);
    }

    public static Specification<Tour> hasStatus(TourStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Tour> hasOrganizerId(UUID organizerId) {
        if (organizerId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("organizer").get("id"), organizerId);
    }
}
