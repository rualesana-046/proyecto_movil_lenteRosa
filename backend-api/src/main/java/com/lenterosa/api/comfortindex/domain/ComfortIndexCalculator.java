package com.lenterosa.api.comfortindex.domain;

import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.comfortindex.ComfortLabel;
import com.lenterosa.api.comfortindex.DimensionComfort;
import com.lenterosa.api.comfortindex.Uncertainty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Cálculo del Índice de Confort (fórmula versión 1, ADR 0003). La fórmula vive solo aquí.
 *
 * <ol>
 *   <li>Cada opinión pesa {@code 0,5 ^ (edad en días / 90)}.</li>
 *   <li>Cada dimensión es un promedio bayesiano con valor previo 3 de peso 3; solo cuentan las
 *       opiniones que la califican (una experiencia nocturna nula no cuenta como 0).</li>
 *   <li>Las cuatro dimensiones pesan 0,25; el peso de la experiencia nocturna se escala por la
 *       parte de la evidencia que la califica y los pesos se renormalizan.</li>
 *   <li>El resultado (1 a 5) se lleva a 0–100 y se redondea a entero; la etiqueta usa ese entero.</li>
 * </ol>
 */
public final class ComfortIndexCalculator {

    public static final int FORMULA_VERSION = 1;

    public static final double HALF_LIFE_DAYS = 90.0;
    static final double PRIOR_MEAN = 3.0;
    static final double PRIOR_WEIGHT = 3.0;
    static final double DIMENSION_WEIGHT = 0.25;

    static final double FEW_OPINIONS_BELOW = 2.0;
    static final double MIXED_DISPERSION_FROM = 1.0;
    static final double NIGHT_EVIDENCE_FROM = 1.0;
    static final int VERY_COMFORTABLE_FROM = 75;
    static final int COMFORTABLE_FROM = 60;
    static final int MIXED_FROM = 40;
    static final double MEDIUM_UNCERTAINTY_FROM = 3.0;
    static final double LOW_UNCERTAINTY_FROM = 8.0;

    private static final double MILLIS_PER_DAY = 86_400_000.0;

    private ComfortIndexCalculator() {
    }

    public static ComfortIndex calculate(List<RatingSample> samples, Instant now) {
        return calculate(RatingAggregates.from(samples, now));
    }

    /** Calcula el índice a partir de las sumas ponderadas; es el camino que usa la API. */
    public static ComfortIndex calculate(RatingAggregates aggregates) {
        if (aggregates.reviewCount() == 0) {
            return new ComfortIndex(null, ComfortLabel.FEW_OPINIONS, Uncertainty.HIGH, 0, 0, true,
                    DimensionComfort.none(), FORMULA_VERSION);
        }
        double effectiveReviews = aggregates.totalWeight();
        double lighting = bayesian(aggregates.lightingWeightedSum(), effectiveReviews);
        double womenPresence = bayesian(aggregates.womenPresenceWeightedSum(), effectiveReviews);
        double commercialVisibility = bayesian(aggregates.commercialVisibilityWeightedSum(), effectiveReviews);
        double nightEvidence = aggregates.nightExperienceWeight();
        double nightExperience = bayesian(aggregates.nightExperienceWeightedSum(), nightEvidence);

        // Opción B: la experiencia nocturna pesa según la parte de la evidencia que la califica.
        double nightWeight = DIMENSION_WEIGHT * nightEvidence / effectiveReviews;
        double totalWeight = 3 * DIMENSION_WEIGHT + nightWeight;
        double combined = (DIMENSION_WEIGHT * (lighting + womenPresence + commercialVisibility)
                + nightWeight * nightExperience) / totalWeight;

        int index = roundIndex((combined - 1) * 25);
        double dispersion = dispersion(aggregates);
        boolean nightDataMissing = nightEvidence < NIGHT_EVIDENCE_FROM;

        DimensionComfort dimensions = new DimensionComfort(lighting, womenPresence, commercialVisibility,
                nightEvidence > 0 ? nightExperience : null);
        return new ComfortIndex(index, label(index, effectiveReviews, dispersion, nightDataMissing),
                uncertainty(effectiveReviews), effectiveReviews, dispersion, nightDataMissing, dimensions,
                FORMULA_VERSION);
    }

    /** Peso por antigüedad. Una opinión con fecha futura cuenta como recién hecha. */
    static double recencyWeight(Instant createdAt, Instant now) {
        double ageDays = Math.max(0, Duration.between(createdAt, now).toMillis() / MILLIS_PER_DAY);
        return Math.pow(0.5, ageDays / HALF_LIFE_DAYS);
    }

    /**
     * Redondea el índice a entero (mitad hacia arriba). Antes se redondea a 6 decimales para que el
     * ruido de punto flotante (74,49999999999999) no cambie la etiqueta de un valor que es 74,5.
     */
    static int roundIndex(double rawIndex) {
        return BigDecimal.valueOf(rawIndex)
                .setScale(6, RoundingMode.HALF_UP)
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }

    /** Etiqueta sobre el índice ya redondeado, con este orden de prioridad. */
    static ComfortLabel label(int index, double effectiveReviews, double dispersion, boolean nightDataMissing) {
        if (effectiveReviews < FEW_OPINIONS_BELOW) {
            return ComfortLabel.FEW_OPINIONS;
        }
        if (dispersion >= MIXED_DISPERSION_FROM) {
            return ComfortLabel.MIXED_OPINIONS;
        }
        if (index >= VERY_COMFORTABLE_FROM) {
            // Sin datos nocturnos suficientes la etiqueta máxima es "Cómodo"; el número no cambia.
            return nightDataMissing ? ComfortLabel.COMFORTABLE : ComfortLabel.VERY_COMFORTABLE;
        }
        if (index >= COMFORTABLE_FROM) {
            return ComfortLabel.COMFORTABLE;
        }
        if (index >= MIXED_FROM) {
            return ComfortLabel.MIXED_OPINIONS;
        }
        return ComfortLabel.UNCOMFORTABLE;
    }

    static Uncertainty uncertainty(double effectiveReviews) {
        if (effectiveReviews < MEDIUM_UNCERTAINTY_FROM) {
            return Uncertainty.HIGH;
        }
        return effectiveReviews < LOW_UNCERTAINTY_FROM ? Uncertainty.MEDIUM : Uncertainty.LOW;
    }

    // Promedio bayesiano con valor previo m = 3 de peso k = 3.
    private static double bayesian(double weightedSum, double evidence) {
        return (PRIOR_WEIGHT * PRIOR_MEAN + weightedSum) / (PRIOR_WEIGHT + evidence);
    }

    // Desviación estándar ponderada del promedio de cada opinión: sqrt(E[x²] − E[x]²).
    private static double dispersion(RatingAggregates aggregates) {
        double mean = aggregates.reviewMeanWeightedSum() / aggregates.totalWeight();
        double variance = aggregates.reviewMeanSquaredWeightedSum() / aggregates.totalWeight() - mean * mean;
        return Math.sqrt(Math.max(0, variance));
    }
}
