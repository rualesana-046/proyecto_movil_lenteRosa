package com.lenterosa.api.reviews.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lenterosa.api.reviews.DecayedRatingSums;
import com.lenterosa.api.reviews.PublicReview;
import com.lenterosa.api.reviews.ReviewRateLimitedException;
import com.lenterosa.api.reviews.ReviewRejectedException;
import com.lenterosa.api.reviews.ReviewSubmission;
import com.lenterosa.api.reviews.domain.Review;
import com.lenterosa.api.reviews.domain.VisitPeriod;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class ReviewSubmissionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final UUID PLACE = UUID.randomUUID();
    private static final UUID OTHER_PLACE = UUID.randomUUID();
    private static final String AUTHOR = "uid-ana";

    private final InMemoryReviews repository = new InMemoryReviews();
    private final ReviewSubmissionService service =
            new ReviewSubmissionService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("Una opinión válida se guarda con su autora y se devuelve anónima")
    void validReviewIsSavedWithAuthor() {
        PublicReview review = service.submit(submission(PLACE, "DAY", 4, null, "Buena iluminación"));

        assertThat(repository.saved).hasSize(1);
        assertThat(repository.saved.getFirst().authorId()).isEqualTo(AUTHOR);
        assertThat(review.lighting()).isEqualTo(4);
        assertThat(review.nightExperience()).isNull();
        assertThat(review.createdAt()).isEqualTo(NOW);
        assertThat(repository.locked).containsExactly(AUTHOR);
    }

    @Test
    @DisplayName("De noche sin experiencia nocturna se rechaza con un mensaje claro")
    void nightWithoutNightExperienceIsRejected() {
        assertThatThrownBy(() -> service.submit(submission(PLACE, "NIGHT", 4, null, null)))
                .isInstanceOf(ReviewRejectedException.class)
                .hasMessage("Si fuiste de noche, califica también la experiencia nocturna.")
                .extracting("reason").isEqualTo(ReviewRejectedException.Reason.INVALID);
    }

    @Test
    @DisplayName("Una calificación fuera de 1 a 5 o un periodo inválido se rechazan")
    void invalidValuesAreRejected() {
        assertThatThrownBy(() -> service.submit(submission(PLACE, "DAY", 6, null, null)))
                .hasMessage("Cada aspecto se califica de 1 a 5.");
        assertThatThrownBy(() -> service.submit(submission(PLACE, "TARDE", 4, null, null)))
                .hasMessage("Indica si fuiste de día o de noche.");
        assertThat(repository.saved).isEmpty();
    }

    @Test
    @DisplayName("Un comentario con un teléfono se rechaza; una dirección colombiana se acepta")
    void contactDataIsRejectedButAddressesAreFine() {
        assertThatThrownBy(() -> service.submit(submission(PLACE, "DAY", 4, null, "Llama al +57 300 123 4567")))
                .isInstanceOf(ReviewRejectedException.class)
                .extracting("reason").isEqualTo(ReviewRejectedException.Reason.CONTACT_DATA);

        service.submit(submission(PLACE, "DAY", 4, null, "Queda en la Calle 18 # 20-30"));

        assertThat(repository.saved).hasSize(1);
    }

    @Test
    @DisplayName("Una segunda opinión del mismo lugar y periodo en 7 días se limita, con el tiempo que falta")
    void samePlaceAndPeriodWithinSevenDaysIsLimited() {
        repository.add(AUTHOR, PLACE, VisitPeriod.DAY, NOW.minus(Duration.ofDays(2)));

        assertThatThrownBy(() -> service.submit(submission(PLACE, "DAY", 4, null, null)))
                .isInstanceOf(ReviewRateLimitedException.class)
                .extracting("retryAfter").isEqualTo(Duration.ofDays(5));
    }

    @Test
    @DisplayName("Otro periodo, otro lugar o pasados 7 días sí se permite")
    void otherPeriodPlaceOrAfterSevenDaysIsAllowed() {
        repository.add(AUTHOR, PLACE, VisitPeriod.DAY, NOW.minus(Duration.ofDays(2)));
        repository.add(AUTHOR, PLACE, VisitPeriod.NIGHT, NOW.minus(Duration.ofDays(7)).minusSeconds(1));

        service.submit(submission(PLACE, "NIGHT", 3, 3, null));
        service.submit(submission(OTHER_PLACE, "DAY", 3, null, null));

        assertThat(repository.saved).hasSize(2);
    }

    @Test
    @DisplayName("La opinión 21 en 24 horas se limita hasta que salga de la ventana la número 20 más reciente")
    void dailyLimitIsTwentyPerTwentyFourHours() {
        for (int i = 0; i < 20; i++) {
            // Opiniones cada 30 minutos sobre lugares distintos, la más antigua hace 10 horas.
            repository.add(AUTHOR, UUID.randomUUID(), VisitPeriod.DAY, NOW.minus(Duration.ofMinutes(600 - 30L * i)));
        }

        assertThatThrownBy(() -> service.submit(submission(PLACE, "DAY", 4, null, null)))
                .isInstanceOf(ReviewRateLimitedException.class)
                .extracting("retryAfter").isEqualTo(Duration.ofHours(14));
    }

    @Test
    @DisplayName("Las opiniones de otra autora no cuentan para el límite")
    void otherAuthorsDoNotCount() {
        repository.add("uid-otra", PLACE, VisitPeriod.DAY, NOW.minus(Duration.ofHours(1)));

        service.submit(submission(PLACE, "DAY", 4, null, null));

        assertThat(repository.saved).hasSize(1);
    }

    private static ReviewSubmission submission(UUID place, String period, int lighting, Integer night, String comment) {
        return new ReviewSubmission(place, AUTHOR, lighting, 4, 3, night, period, comment);
    }

    private record Stored(String authorId, UUID placeId, VisitPeriod period, Instant createdAt) {
    }

    private static final class InMemoryReviews implements ReviewRepository {

        private final List<Stored> existing = new ArrayList<>();
        final List<Stored> saved = new ArrayList<>();
        final List<String> locked = new ArrayList<>();

        void add(String author, UUID place, VisitPeriod period, Instant createdAt) {
            existing.add(new Stored(author, place, period, createdAt));
        }

        @Override
        public void save(Review review, String authorId) {
            Stored stored = new Stored(authorId, review.placeId(), review.visitPeriod(), review.createdAt());
            saved.add(stored);
            existing.add(stored);
        }

        @Override
        public void lockAuthor(String authorId) {
            locked.add(authorId);
        }

        @Override
        public Optional<Instant> oldestReviewByAuthorSince(String authorId, UUID placeId, VisitPeriod period, Instant since) {
            return existing.stream()
                    .filter(r -> r.authorId().equals(authorId) && r.placeId().equals(placeId) && r.period() == period)
                    .map(Stored::createdAt)
                    .filter(t -> t.isAfter(since))
                    .min(Comparator.naturalOrder());
        }

        @Override
        public List<Instant> reviewTimesByAuthorSince(String authorId, Instant since) {
            return existing.stream()
                    .filter(r -> r.authorId().equals(authorId) && r.createdAt().isAfter(since))
                    .map(Stored::createdAt)
                    .sorted()
                    .toList();
        }

        @Override
        public Page<Review> findByPlaceId(UUID placeId, Pageable pageable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RawReviewStats statsForPlace(UUID placeId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int clearAuthor(String authorId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DecayedRatingSums> decayedRatingSums(Collection<UUID> placeIds, Instant now, double halfLifeDays) {
            throw new UnsupportedOperationException();
        }
    }
}
