package com.lenterosa.api.comfortindex.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.lenterosa.api.comfortindex.ComfortIndex;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RatingAggregatesTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");

    @Test
    @DisplayName("Las sumas ponderan cada opinión por su antigüedad y separan la evidencia nocturna")
    void sumsAreWeightedByAge() {
        // Pesos: 1 (hoy) y 0,5 (90 días).
        RatingAggregates aggregates = RatingAggregates.from(List.of(
                new RatingSample(NOW, 4, 5, 3, null),
                new RatingSample(NOW.minus(Duration.ofDays(90)), 2, 2, 2, 2)), NOW);

        assertThat(aggregates.reviewCount()).isEqualTo(2);
        assertThat(aggregates.totalWeight()).isCloseTo(1.5, within(1e-12));
        assertThat(aggregates.lightingWeightedSum()).isCloseTo(4 + 0.5 * 2, within(1e-12));
        assertThat(aggregates.womenPresenceWeightedSum()).isCloseTo(5 + 0.5 * 2, within(1e-12));
        assertThat(aggregates.commercialVisibilityWeightedSum()).isCloseTo(3 + 0.5 * 2, within(1e-12));
        assertThat(aggregates.nightExperienceWeight()).isCloseTo(0.5, within(1e-12));
        assertThat(aggregates.nightExperienceWeightedSum()).isCloseTo(1.0, within(1e-12));
        // Promedios por opinión: 4 (día, tres dimensiones) y 2 (noche, cuatro dimensiones).
        assertThat(aggregates.reviewMeanWeightedSum()).isCloseTo(4 + 0.5 * 2, within(1e-12));
        assertThat(aggregates.reviewMeanSquaredWeightedSum()).isCloseTo(16 + 0.5 * 4, within(1e-12));
    }

    @Test
    @DisplayName("Sin opiniones, las sumas son cero y el índice es nulo")
    void emptyAggregatesGiveNullIndex() {
        ComfortIndex comfort = ComfortIndexCalculator.calculate(RatingAggregates.empty());

        assertThat(comfort.index()).isNull();
        assertThat(comfort.effectiveReviews()).isZero();
    }
}
