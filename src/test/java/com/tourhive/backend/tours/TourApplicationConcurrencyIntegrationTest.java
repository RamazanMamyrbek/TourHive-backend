package com.tourhive.backend.tours;

import com.tourhive.backend.dto.tours.request.ApplyTourRequestDto;
import com.tourhive.backend.dto.tours.request.ReviewApplicationRequestDto;
import com.tourhive.backend.entities.tours.Tour;
import com.tourhive.backend.entities.tours.TourApplication;
import com.tourhive.backend.entities.tours.TourApplicationStatus;
import com.tourhive.backend.entities.tours.TourStatus;
import com.tourhive.backend.entities.tours.TourType;
import com.tourhive.backend.entities.users.Role;
import com.tourhive.backend.entities.users.User;
import com.tourhive.backend.entities.users.UserStatus;
import com.tourhive.backend.exceptions.DuplicateApplicationException;
import com.tourhive.backend.exceptions.TourCapacityExceededException;
import com.tourhive.backend.repositories.tours.TourApplicationRepository;
import com.tourhive.backend.repositories.tours.TourRepository;
import com.tourhive.backend.repositories.users.UserRepository;
import com.tourhive.backend.services.tours.TourApplicationService;
import com.tourhive.backend.support.DatabaseCleanup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("local")
class TourApplicationConcurrencyIntegrationTest {

    @Autowired
    private TourApplicationService tourApplicationService;

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private TourApplicationRepository tourApplicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    private User organizer;
    private User traveler1;
    private User traveler2;
    private Tour tour;

    @BeforeEach
    void setUp() {
        databaseCleanup.clean();

        organizer = User.builder()
                .email("conc_organizer@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(true)
                .build();
        organizer = userRepository.save(organizer);

        traveler1 = User.builder()
                .email("conc_traveler1@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(false)
                .build();
        traveler1 = userRepository.save(traveler1);

        traveler2 = User.builder()
                .email("conc_traveler2@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .isOrganizer(false)
                .build();
        traveler2 = userRepository.save(traveler2);

        // Tour with capacity = 1 and availablePlaces = 1
        tour = Tour.builder()
                .organizer(organizer)
                .title("Single Spot Peak Ascent")
                .description("Extreme climb with only 1 slot")
                .tourType(TourType.COMMUNITY)
                .status(TourStatus.PUBLISHED)
                .startDate(LocalDate.now().plusDays(15))
                .endDate(LocalDate.now().plusDays(20))
                .capacity(1)
                .availablePlaces(1)
                .price(BigDecimal.valueOf(50000))
                .currency("KZT")
                .countries(new ArrayList<>(List.of("Kazakhstan")))
                .cities(new ArrayList<>(List.of("Almaty")))
                .build();
        tour = tourRepository.save(tour);
    }

    @Test
    @DisplayName("concurrent acceptance of the last available spot should accept exactly one application and fail the other")
    void shouldPreventOverbookingUnderConcurrentAcceptance() throws Exception {
        // Both travelers submit applications
        var app1 = tourApplicationService.applyForTour(tour.getId(), traveler1.getId(), new ApplyTourRequestDto("I am ready", null));
        var app2 = tourApplicationService.applyForTour(tour.getId(), traveler2.getId(), new ApplyTourRequestDto("Pick me please", null));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger capacityExceededCount = new AtomicInteger(0);

        ReviewApplicationRequestDto acceptDto = new ReviewApplicationRequestDto(TourApplicationStatus.ACCEPTED, null);

        Future<?> f1 = executor.submit(() -> {
            try {
                startLatch.await();
                tourApplicationService.reviewApplication(tour.getId(), app1.id(), organizer.getId(), acceptDto);
                successCount.incrementAndGet();
            } catch (TourCapacityExceededException e) {
                capacityExceededCount.incrementAndGet();
            } catch (Exception e) {
                // other unexpected
            } finally {
                doneLatch.countDown();
            }
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                startLatch.await();
                tourApplicationService.reviewApplication(tour.getId(), app2.id(), organizer.getId(), acceptDto);
                successCount.incrementAndGet();
            } catch (TourCapacityExceededException e) {
                capacityExceededCount.incrementAndGet();
            } catch (Exception e) {
                // other unexpected
            } finally {
                doneLatch.countDown();
            }
        });

        // Release latch to start both threads simultaneously
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // Verification: Exactly one accepted, exactly one capacity exceeded
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(capacityExceededCount.get()).isEqualTo(1);

        // Verification in database
        Tour reloadedTour = tourRepository.findById(tour.getId()).orElseThrow();
        assertThat(reloadedTour.getAvailablePlaces()).isEqualTo(0);

        List<TourApplication> apps = tourApplicationRepository.findAllByTourId(tour.getId(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        long acceptedCount = apps.stream().filter(a -> a.getStatus() == TourApplicationStatus.ACCEPTED).count();
        long pendingCount = apps.stream().filter(a -> a.getStatus() == TourApplicationStatus.PENDING).count();

        assertThat(acceptedCount).isEqualTo(1);
        assertThat(pendingCount).isEqualTo(1);
    }

    @Test
    @DisplayName("duplicate application by same user should be rejected with DuplicateApplicationException")
    void shouldRejectDuplicateActiveApplication() {
        tourApplicationService.applyForTour(tour.getId(), traveler1.getId(), new ApplyTourRequestDto("First application", null));

        assertThatThrownBy(() -> tourApplicationService.applyForTour(
                tour.getId(),
                traveler1.getId(),
                new ApplyTourRequestDto("Second application", null)
        )).isInstanceOf(DuplicateApplicationException.class)
                .hasMessageContaining("already have an active application");
    }
}
