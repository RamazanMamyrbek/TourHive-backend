package com.tourhive.backend.tours;

import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.spec.tours.TourSpecification;
import com.tourhive.backend.spec.tours.builder.TourSpecificationBuilder;
import com.tourhive.backend.support.DatabaseCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class TourSpecificationIntegrationTest {

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    private User organizer;
    private Tour tour1;
    private Tour tour2;
    private Tour tour3Draft;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        organizer = User.builder()
                .email("spec_organizer@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(true)
                .isBusinessVerified(true)
                .build();
        organizer = userRepository.save(organizer);

        tour1 = Tour.builder()
                .organizer(organizer)
                .title("Kolsay Lakes Adventure")
                .description("Hiking around pristine mountain lakes and scenic trails")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.PUBLISHED)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(8))
                .capacity(10)
                .availablePlaces(10)
                .price(BigDecimal.valueOf(15000))
                .currency("KZT")
                .countries(new ArrayList<>(List.of("Kazakhstan")))
                .cities(new ArrayList<>(List.of("Almaty", "Saty")))
                .build();
        tour1 = tourRepository.save(tour1);

        tour2 = Tour.builder()
                .organizer(organizer)
                .title("Charyn Grand Canyon")
                .description("Red sandstone rock formations exploration")
                .tourType(TourType.COMMERCIAL)
                .status(TourStatus.PUBLISHED)
                .startDate(LocalDate.now().plusDays(10))
                .endDate(LocalDate.now().plusDays(12))
                .capacity(15)
                .availablePlaces(0)
                .price(BigDecimal.valueOf(45000))
                .currency("KZT")
                .countries(new ArrayList<>(List.of("Kazakhstan")))
                .cities(new ArrayList<>(List.of("Almaty")))
                .build();
        tour2 = tourRepository.save(tour2);

        tour3Draft = Tour.builder()
                .organizer(organizer)
                .title("Draft Mountain Trip")
                .description("Secret draft hiking tour")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.DRAFT)
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(25))
                .capacity(8)
                .availablePlaces(8)
                .price(BigDecimal.valueOf(20000))
                .currency("KZT")
                .countries(new ArrayList<>(List.of("Kazakhstan")))
                .cities(new ArrayList<>(List.of("Almaty")))
                .build();
        tour3Draft = tourRepository.save(tour3Draft);
    }

    @Test
    @DisplayName("no optional filters with published visibility should exclude draft tours")
    void shouldReturnOnlyPublishedToursWhenNoFiltersProvided() {
        var spec = new TourSpecificationBuilder()
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> result = tourRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(Tour::getId)
                .containsExactlyInAnyOrder(tour1.getId(), tour2.getId())
                .doesNotContain(tour3Draft.getId());
    }

    @Test
    @DisplayName("search should match substring in title or description case-insensitively")
    void shouldSearchByTitleOrDescription() {
        // "hiking" is in tour1's description
        var spec1 = new TourSpecificationBuilder()
                .withSearch("hiking")
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> result1 = tourRepository.findAll(spec1, PageRequest.of(0, 10));
        assertThat(result1.getContent()).extracting(Tour::getId).containsExactly(tour1.getId());

        // "canyon" is in tour2's title
        var spec2 = new TourSpecificationBuilder()
                .withSearch("CANYON")
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> result2 = tourRepository.findAll(spec2, PageRequest.of(0, 10));
        assertThat(result2.getContent()).extracting(Tour::getId).containsExactly(tour2.getId());
    }

    @Test
    @DisplayName("wildcard characters in search should be escaped and treated literally")
    void shouldEscapeWildcardCharactersInSearch() {
        var spec = new TourSpecificationBuilder()
                .withSearch("%_")
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> result = tourRepository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("country and city joins should not produce duplicate rows")
    void shouldNotProduceDuplicateRowsForMultiCityTours() {
        var spec = new TourSpecificationBuilder()
                .withCountry("Kazakhstan")
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> result = tourRepository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(2);

        var specCity = new TourSpecificationBuilder()
                .withCity("Saty")
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> resultCity = tourRepository.findAll(specCity, PageRequest.of(0, 10));
        assertThat(resultCity.getContent()).extracting(Tour::getId).containsExactly(tour1.getId());
    }

    @Test
    @DisplayName("tourType filter should match exact enum")
    void shouldFilterByTourType() {
        var specCommunity = new TourSpecificationBuilder()
                .withTourType(TourType.COMMUNITY)
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> communityResult = tourRepository.findAll(specCommunity, PageRequest.of(0, 10));
        assertThat(communityResult.getContent()).extracting(Tour::getId).containsExactly(tour1.getId());

        var specCommercial = new TourSpecificationBuilder()
                .withTourType(TourType.COMMERCIAL)
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> commercialResult = tourRepository.findAll(specCommercial, PageRequest.of(0, 10));
        assertThat(commercialResult.getContent()).extracting(Tour::getId).containsExactly(tour2.getId());
    }

    @Test
    @DisplayName("date range filters should bound start date correctly")
    void shouldFilterByDateRange() {
        var specFrom = new TourSpecificationBuilder()
                .withDateFrom(LocalDate.now().plusDays(7))
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> fromResult = tourRepository.findAll(specFrom, PageRequest.of(0, 10));
        assertThat(fromResult.getContent()).extracting(Tour::getId).containsExactly(tour2.getId());

        var specTo = new TourSpecificationBuilder()
                .withDateTo(LocalDate.now().plusDays(7))
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> toResult = tourRepository.findAll(specTo, PageRequest.of(0, 10));
        assertThat(toResult.getContent()).extracting(Tour::getId).containsExactly(tour1.getId());
    }

    @Test
    @DisplayName("price range filters should filter inclusive boundaries")
    void shouldFilterByPriceRange() {
        var spec = new TourSpecificationBuilder()
                .withMinPrice(BigDecimal.valueOf(20000))
                .withMaxPrice(BigDecimal.valueOf(50000))
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> result = tourRepository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(Tour::getId).containsExactly(tour2.getId());
    }

    @Test
    @DisplayName("hasAvailablePlaces should exclude full tours")
    void shouldFilterByAvailablePlaces() {
        var spec = new TourSpecificationBuilder()
                .withHasAvailablePlaces(true)
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> result = tourRepository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(Tour::getId).containsExactly(tour1.getId());
    }

    @Test
    @DisplayName("combined AND filters should narrow down to exact matching tour")
    void shouldCombineFiltersWithAnd() {
        var spec = new TourSpecificationBuilder()
                .withCountry("Kazakhstan")
                .withCity("Almaty")
                .withTourType(TourType.COMMERCIAL)
                .withMinPrice(BigDecimal.valueOf(30000))
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> result = tourRepository.findAll(spec, PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(Tour::getId).containsExactly(tour2.getId());
    }

    @Test
    @DisplayName("pagination and sorting should sort results properly")
    void shouldPaginateAndSortCorrectly() {
        var spec = new TourSpecificationBuilder()
                .build()
                .and(TourSpecification.hasStatus(TourStatus.PUBLISHED));

        Page<Tour> pageAsc = tourRepository.findAll(spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "price")));
        assertThat(pageAsc.getContent()).extracting(Tour::getId).containsExactly(tour1.getId(), tour2.getId());

        Page<Tour> pageDesc = tourRepository.findAll(spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "price")));
        assertThat(pageDesc.getContent()).extracting(Tour::getId).containsExactly(tour2.getId(), tour1.getId());
    }
}
