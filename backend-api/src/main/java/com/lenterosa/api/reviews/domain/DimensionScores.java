package com.lenterosa.api.reviews.domain;

import java.util.Objects;

/**
 * Calificaciones de una opinión en sus cuatro dimensiones.
 *
 * <p>Iluminación, afluencia de mujeres y visibilidad comercial son obligatorias.
 * La experiencia nocturna puede faltar: si es obligatoria o no depende del periodo de la visita,
 * y esa regla vive en {@link Review}.
 */
public record DimensionScores(Rating lighting, Rating womenPresence, Rating commercialVisibility, Rating nightExperience) {

    public DimensionScores {
        Objects.requireNonNull(lighting, "lighting");
        Objects.requireNonNull(womenPresence, "womenPresence");
        Objects.requireNonNull(commercialVisibility, "commercialVisibility");
    }

    /** Crea las calificaciones desde enteros; {@code nightExperience} nulo significa "No fui de noche". */
    public static DimensionScores of(int lighting, int womenPresence, int commercialVisibility, Integer nightExperience) {
        return new DimensionScores(
                new Rating(lighting),
                new Rating(womenPresence),
                new Rating(commercialVisibility),
                nightExperience == null ? null : new Rating(nightExperience));
    }

    public boolean hasNightExperience() {
        return nightExperience != null;
    }
}
