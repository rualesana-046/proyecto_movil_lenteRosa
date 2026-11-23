package com.lenterosa.api.comfortindex.domain;

import java.time.Instant;
import java.util.List;

/**
 * Sumas ponderadas por antigüedad de las opiniones de un lugar: todo lo que necesita el cálculo
 * del índice, en tamaño fijo, sin importar cuántas opiniones haya.
 *
 * <p>Iluminación, afluencia y visibilidad son obligatorias, así que su evidencia es {@code totalWeight}.
 * La experiencia nocturna tiene su propia evidencia porque puede faltar. El promedio de cada
 * opinión ({@code reviewMean}) usa solo sus dimensiones presentes y sirve para la dispersión.
 */
public record RatingAggregates(
        long reviewCount,
        double totalWeight,
        double lightingWeightedSum,
        double womenPresenceWeightedSum,
        double commercialVisibilityWeightedSum,
        double nightExperienceWeight,
        double nightExperienceWeightedSum,
        double reviewMeanWeightedSum,
        double reviewMeanSquaredWeightedSum) {

    public static RatingAggregates empty() {
        return new RatingAggregates(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    /** Calcula las sumas en memoria; es la referencia con que se comparan las sumas hechas en la base. */
    public static RatingAggregates from(List<RatingSample> samples, Instant now) {
        double totalWeight = 0;
        double lighting = 0;
        double womenPresence = 0;
        double commercialVisibility = 0;
        double nightWeight = 0;
        double night = 0;
        double meanSum = 0;
        double meanSquaredSum = 0;
        for (RatingSample sample : samples) {
            double weight = ComfortIndexCalculator.recencyWeight(sample.createdAt(), now);
            totalWeight += weight;
            lighting += weight * sample.lighting();
            womenPresence += weight * sample.womenPresence();
            commercialVisibility += weight * sample.commercialVisibility();
            if (sample.nightExperience() != null) {
                nightWeight += weight;
                night += weight * sample.nightExperience();
            }
            double mean = reviewMean(sample);
            meanSum += weight * mean;
            meanSquaredSum += weight * mean * mean;
        }
        return new RatingAggregates(samples.size(), totalWeight, lighting, womenPresence, commercialVisibility,
                nightWeight, night, meanSum, meanSquaredSum);
    }

    private static double reviewMean(RatingSample sample) {
        int sum = sample.lighting() + sample.womenPresence() + sample.commercialVisibility();
        return sample.nightExperience() == null ? sum / 3.0 : (sum + sample.nightExperience()) / 4.0;
    }
}
