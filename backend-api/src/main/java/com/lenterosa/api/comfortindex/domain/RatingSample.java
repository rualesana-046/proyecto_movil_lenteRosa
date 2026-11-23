package com.lenterosa.api.comfortindex.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Calificaciones de una opinión, tal como las usa el cálculo del índice.
 *
 * @param nightExperience nula cuando la opinión no califica la experiencia nocturna
 */
public record RatingSample(
        Instant createdAt, int lighting, int womenPresence, int commercialVisibility, Integer nightExperience) {

    public RatingSample {
        Objects.requireNonNull(createdAt, "createdAt");
        requireRating(lighting);
        requireRating(womenPresence);
        requireRating(commercialVisibility);
        if (nightExperience != null) {
            requireRating(nightExperience);
        }
    }

    private static void requireRating(int value) {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("La calificación debe estar entre 1 y 5: " + value);
        }
    }
}
