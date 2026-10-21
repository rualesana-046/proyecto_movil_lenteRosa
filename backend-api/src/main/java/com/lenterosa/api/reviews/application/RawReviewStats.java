package com.lenterosa.api.reviews.application;

import java.time.Instant;

/**
 * Agregados tal como los calcula la base: promedios sin redondear y nulos cuando no hay datos.
 * El promedio de la experiencia nocturna ignora las opiniones que no la califican.
 */
public record RawReviewStats(
        long reviewCount,
        Instant lastReviewAt,
        Double lightingAverage,
        Double womenPresenceAverage,
        Double commercialVisibilityAverage,
        Double nightExperienceAverage) {
}
