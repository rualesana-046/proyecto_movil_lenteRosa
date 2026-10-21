package com.lenterosa.api.reviews.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DimensionScoresTest {

    @Test
    @DisplayName("Cada valor llega a su dimensión, sin intercambios")
    void eachValueLandsOnItsDimension() {
        // Valores distintos en cada posición para detectar cualquier intercambio.
        DimensionScores scores = DimensionScores.of(1, 2, 3, 4);

        assertThat(scores.lighting().value()).isEqualTo(1);
        assertThat(scores.womenPresence().value()).isEqualTo(2);
        assertThat(scores.commercialVisibility().value()).isEqualTo(3);
        assertThat(scores.nightExperience().value()).isEqualTo(4);
    }

    @Test
    @DisplayName("La experiencia nocturna puede faltar (\"No fui de noche\")")
    void nightExperienceMayBeMissing() {
        DimensionScores scores = DimensionScores.of(4, 4, 3, null);

        assertThat(scores.nightExperience()).isNull();
        assertThat(scores.hasNightExperience()).isFalse();
    }

    @Test
    @DisplayName("Las tres dimensiones obligatorias no pueden faltar")
    void requiredDimensionsCannotBeMissing() {
        Rating three = new Rating(3);

        assertThatNullPointerException().isThrownBy(() -> new DimensionScores(null, three, three, three));
        assertThatNullPointerException().isThrownBy(() -> new DimensionScores(three, null, three, three));
        assertThatNullPointerException().isThrownBy(() -> new DimensionScores(three, three, null, three));
    }

    @Test
    @DisplayName("Una dimensión fuera de 1 a 5 se rechaza, incluida la nocturna")
    void rejectsOutOfRangeValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> DimensionScores.of(0, 3, 3, 3));
        assertThatIllegalArgumentException().isThrownBy(() -> DimensionScores.of(3, 6, 3, 3));
        assertThatIllegalArgumentException().isThrownBy(() -> DimensionScores.of(3, 3, 0, 3));
        assertThatIllegalArgumentException().isThrownBy(() -> DimensionScores.of(3, 3, 3, 6));
    }
}
