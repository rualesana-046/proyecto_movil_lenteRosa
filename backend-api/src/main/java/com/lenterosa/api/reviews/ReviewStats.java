package com.lenterosa.api.reviews;

import java.time.Instant;

/**
 * Resumen de las opiniones de un lugar.
 *
 * @param reviewCount cantidad de opiniones
 * @param lastReviewAt fecha de la opinión más reciente; nula si no hay opiniones
 * @param averages promedio por dimensión, redondeado a un decimal
 */
public record ReviewStats(long reviewCount, Instant lastReviewAt, DimensionAverages averages) {

    public static ReviewStats empty() {
        return new ReviewStats(0, null, DimensionAverages.none());
    }
}
