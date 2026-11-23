package com.lenterosa.api.comfortindex.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.comfortindex.ComfortLabel;
import com.lenterosa.api.reviews.DecayedRatingSums;
import com.lenterosa.api.reviews.PublicReview;
import com.lenterosa.api.reviews.ReviewQueries;
import com.lenterosa.api.reviews.ReviewRatings;
import com.lenterosa.api.reviews.ReviewStats;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class ComfortIndexServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final UUID PARK = UUID.randomUUID();
    private static final UUID STREET = UUID.randomUUID();
    private static final UUID EMPTY = UUID.randomUUID();

    @Test
    @DisplayName("Agrupa por lugar con una sola consulta e incluye los lugares sin opiniones")
    void groupsByPlaceWithOneQuery() {
        FakeReviews reviews = new FakeReviews(List.of(
                ratings(PARK, 1, 5, 5, 5, 5), ratings(PARK, 1, 5, 5, 5, 5), ratings(PARK, 1, 5, 5, 5, 5),
                ratings(PARK, 1, 5, 5, 5, 5), ratings(PARK, 1, 5, 5, 5, 5),
                ratings(STREET, 10, 1, 2, 1, 1)));
        ComfortIndexService service = new ComfortIndexService(reviews, Clock.fixed(NOW, ZoneOffset.UTC));

        Map<UUID, ComfortIndex> result = service.forPlaces(List.of(PARK, STREET, EMPTY));

        assertThat(reviews.queries()).isEqualTo(1);
        assertThat(result).containsOnlyKeys(PARK, STREET, EMPTY);
        assertThat(result.get(PARK).index()).isEqualTo(81);
        assertThat(result.get(PARK).label()).isEqualTo(ComfortLabel.VERY_COMFORTABLE);
        assertThat(result.get(STREET).index()).isEqualTo(40);
        assertThat(result.get(EMPTY).index()).isNull();
    }

    @Test
    @DisplayName("Usa el reloj inyectado: el mismo dato pesa menos con el paso del tiempo")
    void usesInjectedClock() {
        FakeReviews reviews = new FakeReviews(Collections.nCopies(5, ratings(PARK, 1, 5, 5, 5, 5)));

        ComfortIndex now = new ComfortIndexService(reviews, Clock.fixed(NOW, ZoneOffset.UTC)).forPlace(PARK);
        ComfortIndex inAYear = new ComfortIndexService(reviews, Clock.fixed(NOW.plus(Duration.ofDays(365)), ZoneOffset.UTC))
                .forPlace(PARK);

        assertThat(now.effectiveReviews()).isGreaterThan(inAYear.effectiveReviews());
        assertThat(inAYear.label()).isEqualTo(ComfortLabel.FEW_OPINIONS);
    }

    private static ReviewRatings ratings(UUID placeId, int ageDays, int lighting, int womenPresence,
            int commercialVisibility, Integer nightExperience) {
        return new ReviewRatings(placeId, lighting, womenPresence, commercialVisibility, nightExperience,
                NOW.minus(Duration.ofDays(ageDays)));
    }

    private static final class FakeReviews implements ReviewQueries {

        private final List<ReviewRatings> ratings;
        private int queries;

        FakeReviews(List<ReviewRatings> ratings) {
            this.ratings = ratings;
        }

        int queries() {
            return queries;
        }

        @Override
        public List<ReviewRatings> ratingsForPlaces(Collection<UUID> placeIds) {
            queries++;
            return ratings.stream().filter(r -> placeIds.contains(r.placeId())).toList();
        }

        @Override
        public ReviewStats statsForPlace(UUID placeId) {
            return ReviewStats.empty();
        }

        @Override
        public Page<PublicReview> listForPlace(UUID placeId, Pageable pageable) {
            return Page.empty(pageable);
        }

        @Override
        public List<DecayedRatingSums> decayedRatingSums(Collection<UUID> placeIds, Instant now, double halfLifeDays) {
            return List.of();
        }
    }
}
