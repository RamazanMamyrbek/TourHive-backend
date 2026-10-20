package com.tourhive.backend.dto.tours.request;

import com.tourhive.backend.entities.tours.TourType;
import io.swagger.v3.oas.annotations.Parameter;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TourSearchCriteria(
        @Parameter(description = "Partial search term matching title or description")
        String search,

        @Parameter(description = "Destination country")
        String country,

        @Parameter(description = "Destination city")
        String city,

        @Parameter(description = "Tour type (COMMUNITY or COMMERCIAL)")
        TourType tourType,

        @Parameter(description = "Earliest tour start date (inclusive)")
        LocalDate dateFrom,

        @Parameter(description = "Latest tour start date (inclusive)")
        LocalDate dateTo,

        @Parameter(description = "Minimum tour price")
        BigDecimal minPrice,

        @Parameter(description = "Maximum tour price")
        BigDecimal maxPrice,

        @Parameter(description = "Only return tours with available capacity (availablePlaces > 0)")
        Boolean hasAvailablePlaces
) {
}
