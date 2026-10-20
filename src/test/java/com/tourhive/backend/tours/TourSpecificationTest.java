package com.tourhive.backend.tours;

import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.spec.tours.TourSpecification;
import com.tourhive.backend.spec.tours.builder.TourSpecificationBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TourSpecificationTest {

    @Test
    @DisplayName("static factory methods should return null for null/blank inputs")
    void shouldReturnNullForAbsentInputs() {
        assertThat(TourSpecification.search(null)).isNull();
        assertThat(TourSpecification.search("   ")).isNull();
        assertThat(TourSpecification.hasCountry(null)).isNull();
        assertThat(TourSpecification.hasCountry("  ")).isNull();
        assertThat(TourSpecification.hasCity(null)).isNull();
        assertThat(TourSpecification.hasCity("  ")).isNull();
        assertThat(TourSpecification.hasTourType(null)).isNull();
        assertThat(TourSpecification.startDateFrom(null)).isNull();
        assertThat(TourSpecification.startDateTo(null)).isNull();
        assertThat(TourSpecification.priceGte(null)).isNull();
        assertThat(TourSpecification.priceLte(null)).isNull();
        assertThat(TourSpecification.hasAvailablePlaces(null)).isNull();
        assertThat(TourSpecification.hasAvailablePlaces(false)).isNull();
        assertThat(TourSpecification.hasStatus(null)).isNull();
        assertThat(TourSpecification.hasOrganizerId(null)).isNull();
    }

    @Test
    @DisplayName("static factory methods should return non-null Specification when parameters provided")
    void shouldReturnSpecificationWhenPresent() {
        assertThat(TourSpecification.search("Kolsay")).isNotNull();
        assertThat(TourSpecification.hasCountry("Kazakhstan")).isNotNull();
        assertThat(TourSpecification.hasCity("Almaty")).isNotNull();
        assertThat(TourSpecification.hasTourType(TourType.COMMUNITY)).isNotNull();
        assertThat(TourSpecification.startDateFrom(LocalDate.now())).isNotNull();
        assertThat(TourSpecification.startDateTo(LocalDate.now().plusDays(5))).isNotNull();
        assertThat(TourSpecification.priceGte(BigDecimal.valueOf(1000))).isNotNull();
        assertThat(TourSpecification.priceLte(BigDecimal.valueOf(50000))).isNotNull();
        assertThat(TourSpecification.hasAvailablePlaces(true)).isNotNull();
        assertThat(TourSpecification.hasStatus(TourStatus.PUBLISHED)).isNotNull();
        assertThat(TourSpecification.hasOrganizerId(UUID.randomUUID())).isNotNull();
    }

    @Test
    @DisplayName("TourSpecificationBuilder fluent chaining should compose correctly")
    void shouldBuildComposedSpecification() {
        Specification<Tour> spec = new TourSpecificationBuilder()
                .withSearch("Trek")
                .withCountry("Kazakhstan")
                .withCity("Almaty")
                .withTourType(TourType.COMMERCIAL)
                .withDateFrom(LocalDate.now())
                .withDateTo(LocalDate.now().plusDays(10))
                .withMinPrice(BigDecimal.valueOf(5000))
                .withMaxPrice(BigDecimal.valueOf(25000))
                .withHasAvailablePlaces(true)
                .withStatus(TourStatus.PUBLISHED)
                .build();

        assertThat(spec).isNotNull();
    }
}
