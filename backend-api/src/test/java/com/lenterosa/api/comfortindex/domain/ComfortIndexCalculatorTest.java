package com.lenterosa.api.comfortindex.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.comfortindex.ComfortLabel;
import com.lenterosa.api.comfortindex.Uncertainty;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// Los valores esperados se calcularon a mano y con una implementación de referencia independiente
// de la fórmula del ADR 0003.
class ComfortIndexCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final double TOLERANCE = 1e-4;

    @Nested
    @DisplayName("Fórmula")
    class Formula {

        @Test
        @DisplayName("Sin opiniones: índice nulo, Pocas opiniones, incertidumbre alta y dimensiones nulas")
        void noReviews() {
            ComfortIndex comfort = ComfortIndexCalculator.calculate(List.of(), NOW);

            assertThat(comfort.index()).isNull();
            assertThat(comfort.label()).isEqualTo(ComfortLabel.FEW_OPINIONS);
            assertThat(comfort.uncertainty()).isEqualTo(Uncertainty.HIGH);
            assertThat(comfort.effectiveReviews()).isZero();
            assertThat(comfort.nightDataMissing()).isTrue();
            assertThat(comfort.dimensions().lighting()).isNull();
            assertThat(comfort.dimensions().nightExperience()).isNull();
            assertThat(comfort.formulaVersion()).isEqualTo(1);
        }

        @ParameterizedTest(name = "{0} días → peso {1}")
        @CsvSource({"0, 1.0", "30, 0.7937", "90, 0.5", "180, 0.25", "365, 0.0601"})
        @DisplayName("El peso se reduce a la mitad cada 90 días")
        void recencyWeightHalvesEveryNinetyDays(int ageDays, double expectedWeight) {
            assertThat(ComfortIndexCalculator.recencyWeight(daysAgo(ageDays), NOW)).isCloseTo(expectedWeight, within(TOLERANCE));
        }

        @Test
        @DisplayName("Una opinión con fecha futura cuenta como recién hecha")
        void futureReviewCountsAsNew() {
            assertThat(ComfortIndexCalculator.recencyWeight(NOW.plusSeconds(3_600), NOW)).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Promedio bayesiano por dimensión, calculado a mano (Parque de dos opiniones)")
        void bayesianAveragePerDimension() {
            // Pesos: 0,5^(20/90) = 0,8572 y 0,5^(3/90) = 0,9772.
            // Iluminación: (3·3 + 0,8572·4 + 0,9772·2) / (3 + 1,8344) = 2,9752.
            // Nocturna: solo la opinión de noche → (9 + 0,9772·2) / 3,9772 = 2,7543.
            ComfortIndex comfort = ComfortIndexCalculator.calculate(List.of(
                    sample(20, 4, 4, 3, null),
                    sample(3, 2, 2, 2, 2)), NOW);

            assertThat(comfort.dimensions().lighting()).isCloseTo(2.9752, within(TOLERANCE));
            assertThat(comfort.dimensions().womenPresence()).isCloseTo(2.9752, within(TOLERANCE));
            assertThat(comfort.dimensions().commercialVisibility()).isCloseTo(2.7979, within(TOLERANCE));
            assertThat(comfort.dimensions().nightExperience()).isCloseTo(2.7543, within(TOLERANCE));
            assertThat(comfort.effectiveReviews()).isCloseTo(1.8344, within(TOLERANCE));
            assertThat(comfort.dispersion()).isCloseTo(0.8316, within(TOLERANCE));
            assertThat(comfort.index()).isEqualTo(47);
            assertThat(comfort.nightDataMissing()).isTrue();
        }

        @Test
        @DisplayName("La experiencia nocturna nula no cuenta como 0 ni pesa en el índice")
        void missingNightExperienceIsNotZero() {
            ComfortIndex comfort = ComfortIndexCalculator.calculate(List.of(sample(1, 5, 4, 5, null)), NOW);

            assertThat(comfort.dimensions().nightExperience()).isNull();
            // Promedio de las tres dimensiones de día: (3,4971 + 3,2486 + 3,4971) / 3 = 3,4143 → 60.
            assertThat(comfort.index()).isEqualTo(60);
            assertThat(comfort.nightDataMissing()).isTrue();
        }

        @Test
        @DisplayName("Opiniones de un año valen poco: seis notas de 5 no bastan")
        void oldReviewsWeighLittle() {
            ComfortIndex comfort = ComfortIndexCalculator.calculate(Collections.nCopies(6, sample(365, 5, 5, 5, 5)), NOW);

            assertThat(comfort.effectiveReviews()).isCloseTo(0.3608, within(TOLERANCE));
            assertThat(comfort.index()).isEqualTo(55);
            assertThat(comfort.label()).isEqualTo(ComfortLabel.FEW_OPINIONS);
        }

        @Test
        @DisplayName("Una calificación fuera de 1 a 5 se rechaza")
        void rejectsInvalidRating() {
            assertThatIllegalArgumentException().isThrownBy(() -> sample(1, 0, 3, 3, null));
            assertThatIllegalArgumentException().isThrownBy(() -> sample(1, 3, 3, 3, 6));
        }
    }

    @Nested
    @DisplayName("Regresión con los ejemplos del diseño")
    class DesignExamples {

        @Test
        @DisplayName("Datos ficticios de una opinión: Parada 52, Calle 40, Bar 55, todos con Pocas opiniones")
        void singleReviewPlaces() {
            assertIndex(List.of(sample(7, 3, 3, 4, 3)), 52, ComfortLabel.FEW_OPINIONS, Uncertainty.HIGH);
            assertIndex(List.of(sample(10, 1, 2, 1, 1)), 40, ComfortLabel.FEW_OPINIONS, Uncertainty.HIGH);
            assertIndex(List.of(sample(2, 3, 4, 4, 4)), 55, ComfortLabel.FEW_OPINIONS, Uncertainty.HIGH);
        }

        @Test
        @DisplayName("Cinco recientes en 5 → 81, Muy cómodo, incertidumbre media")
        void fiveRecentTopRatings() {
            assertIndex(Collections.nCopies(5, sample(1, 5, 5, 5, 5)), 81, ComfortLabel.VERY_COMFORTABLE, Uncertainty.MEDIUM);
        }

        @Test
        @DisplayName("Diez recientes en 4, solo de día → 69, Cómodo, incertidumbre baja")
        void tenDayOnlyReviews() {
            assertIndex(Collections.nCopies(10, sample(1, 4, 4, 4, null)), 69, ComfortLabel.COMFORTABLE, Uncertainty.LOW);
        }

        @Test
        @DisplayName("Seis recientes en 1 → 17, Poco cómodo")
        void sixRecentLowRatings() {
            assertIndex(Collections.nCopies(6, sample(1, 1, 1, 1, 1)), 17, ComfortLabel.UNCOMFORTABLE, Uncertainty.MEDIUM);
        }

        @Test
        @DisplayName("Cuatro en 5 y cuatro en 1 → 50, Opiniones mixtas por dispersión 2,0")
        void strongDisagreement() {
            List<RatingSample> samples = new ArrayList<>(Collections.nCopies(4, sample(1, 5, 5, 5, 5)));
            samples.addAll(Collections.nCopies(4, sample(1, 1, 1, 1, 1)));

            ComfortIndex comfort = assertIndex(samples, 50, ComfortLabel.MIXED_OPINIONS, Uncertainty.MEDIUM);
            assertThat(comfort.dispersion()).isCloseTo(2.0, within(TOLERANCE));
        }
    }

    @Nested
    @DisplayName("Tope de etiqueta sin datos nocturnos")
    class NightDataCap {

        private final List<RatingSample> dayOnly = List.of(
                sample(1, 5, 5, 5, null), sample(3, 5, 5, 5, null), sample(5, 5, 5, 4, null),
                sample(8, 5, 5, 5, null), sample(13, 5, 4, 5, null));

        @Test
        @DisplayName("Solo visitas de día con índice 79 → Cómodo, nunca Muy cómodo; el número no cambia")
        void dayOnlyIsCappedAtComfortable() {
            ComfortIndex comfort = ComfortIndexCalculator.calculate(dayOnly, NOW);

            assertThat(comfort.index()).isEqualTo(79);
            assertThat(comfort.nightDataMissing()).isTrue();
            assertThat(comfort.label()).isEqualTo(ComfortLabel.COMFORTABLE);
        }

        @Test
        @DisplayName("Las mismas opiniones con experiencia nocturna → Muy cómodo")
        void sameReviewsWithNightDataAreVeryComfortable() {
            List<RatingSample> withNight = dayOnly.stream()
                    .map(s -> new RatingSample(s.createdAt(), s.lighting(), s.womenPresence(), s.commercialVisibility(), 5))
                    .toList();

            ComfortIndex comfort = ComfortIndexCalculator.calculate(withNight, NOW);

            assertThat(comfort.index()).isEqualTo(79);
            assertThat(comfort.nightDataMissing()).isFalse();
            assertThat(comfort.label()).isEqualTo(ComfortLabel.VERY_COMFORTABLE);
        }

        @Test
        @DisplayName("El tope no afecta a otras etiquetas")
        void capOnlyAffectsVeryComfortable() {
            assertThat(ComfortIndexCalculator.label(80, 5, 0, true)).isEqualTo(ComfortLabel.COMFORTABLE);
            assertThat(ComfortIndexCalculator.label(80, 5, 0, false)).isEqualTo(ComfortLabel.VERY_COMFORTABLE);
            assertThat(ComfortIndexCalculator.label(65, 5, 0, true)).isEqualTo(ComfortLabel.COMFORTABLE);
            assertThat(ComfortIndexCalculator.label(30, 5, 0, true)).isEqualTo(ComfortLabel.UNCOMFORTABLE);
        }
    }

    @Nested
    @DisplayName("Redondeo y umbrales")
    class Thresholds {

        @ParameterizedTest(name = "{0} → {1} → {2}")
        @CsvSource({
            "74.5, 75, VERY_COMFORTABLE",
            "74.4, 74, COMFORTABLE",
            "74.49999999999999, 75, VERY_COMFORTABLE",
            "59.5, 60, COMFORTABLE",
            "59.4, 59, MIXED_OPINIONS",
            "39.5, 40, MIXED_OPINIONS",
            "39.4, 39, UNCOMFORTABLE"
        })
        @DisplayName("La etiqueta usa el índice ya redondeado a entero")
        void labelUsesRoundedIndex(double rawIndex, int expectedIndex, ComfortLabel expectedLabel) {
            int index = ComfortIndexCalculator.roundIndex(rawIndex);

            assertThat(index).isEqualTo(expectedIndex);
            assertThat(ComfortIndexCalculator.label(index, 5, 0, false)).isEqualTo(expectedLabel);
        }

        @ParameterizedTest(name = "n_eff {0} → {1}")
        @CsvSource({"1.99, FEW_OPINIONS", "2.0, VERY_COMFORTABLE"})
        @DisplayName("Pocas opiniones por debajo de 2 opiniones efectivas")
        void fewOpinionsThreshold(double effectiveReviews, ComfortLabel expected) {
            assertThat(ComfortIndexCalculator.label(90, effectiveReviews, 0, false)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "dispersión {0} → {1}")
        @CsvSource({"0.99, VERY_COMFORTABLE", "1.0, MIXED_OPINIONS"})
        @DisplayName("Opiniones mixtas desde dispersión 1,0, aunque el promedio sea alto")
        void mixedOpinionsThreshold(double dispersion, ComfortLabel expected) {
            assertThat(ComfortIndexCalculator.label(90, 5, dispersion, false)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Pocas opiniones tiene prioridad sobre Opiniones mixtas")
        void fewOpinionsHasPriority() {
            assertThat(ComfortIndexCalculator.label(50, 1.5, 2.0, false)).isEqualTo(ComfortLabel.FEW_OPINIONS);
        }

        @ParameterizedTest(name = "n_eff {0} → {1}")
        @CsvSource({"2.99, HIGH", "3.0, MEDIUM", "7.99, MEDIUM", "8.0, LOW"})
        @DisplayName("Incertidumbre alta por debajo de 3, media por debajo de 8 y baja desde 8")
        void uncertaintyThresholds(double effectiveReviews, Uncertainty expected) {
            assertThat(ComfortIndexCalculator.uncertainty(effectiveReviews)).isEqualTo(expected);
        }
    }

    private static ComfortIndex assertIndex(List<RatingSample> samples, int index, ComfortLabel label,
            Uncertainty uncertainty) {
        ComfortIndex comfort = ComfortIndexCalculator.calculate(samples, NOW);
        assertThat(comfort.index()).isEqualTo(index);
        assertThat(comfort.label()).isEqualTo(label);
        assertThat(comfort.uncertainty()).isEqualTo(uncertainty);
        return comfort;
    }

    private static RatingSample sample(int ageDays, int lighting, int womenPresence, int commercialVisibility,
            Integer nightExperience) {
        return new RatingSample(daysAgo(ageDays), lighting, womenPresence, commercialVisibility, nightExperience);
    }

    private static Instant daysAgo(int days) {
        return NOW.minus(Duration.ofDays(days));
    }
}
