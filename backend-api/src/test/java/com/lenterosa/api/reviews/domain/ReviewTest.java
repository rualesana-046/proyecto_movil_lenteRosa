package com.lenterosa.api.reviews.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewTest {

    @Test
    @DisplayName("Una visita de noche sin experiencia nocturna se rechaza")
    void nightVisitWithoutNightExperienceIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> review(DimensionScores.of(2, 3, 2, null), VisitPeriod.NIGHT));
    }

    @Test
    @DisplayName("Una visita de noche con experiencia nocturna se acepta")
    void nightVisitWithNightExperienceIsAccepted() {
        Review review = review(DimensionScores.of(2, 3, 2, 2), VisitPeriod.NIGHT);

        assertThat(review.scores().nightExperience().value()).isEqualTo(2);
    }

    @Test
    @DisplayName("Una visita de día sin experiencia nocturna se acepta")
    void dayVisitWithoutNightExperienceIsAccepted() {
        Review review = review(DimensionScores.of(4, 4, 3, null), VisitPeriod.DAY);

        assertThat(review.scores().hasNightExperience()).isFalse();
    }

    @Test
    @DisplayName("Una visita de día también puede calificar la experiencia nocturna")
    void dayVisitMayIncludeNightExperience() {
        Review review = review(DimensionScores.of(4, 4, 3, 3), VisitPeriod.DAY);

        assertThat(review.scores().nightExperience().value()).isEqualTo(3);
    }

    private static Review review(DimensionScores scores, VisitPeriod period) {
        return new Review(UUID.randomUUID(), UUID.randomUUID(), scores, period, null, Instant.now());
    }
}
