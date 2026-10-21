package com.lenterosa.api.reviews;

/**
 * Promedio de cada dimensión, redondeado a un decimal.
 *
 * <p>Un promedio es nulo cuando no hay datos: sin opiniones, o, para la experiencia nocturna,
 * sin opiniones que la califiquen. Nunca se usa 0 para "sin datos".
 */
public record DimensionAverages(Double lighting, Double womenPresence, Double commercialVisibility, Double nightExperience) {

    public static DimensionAverages none() {
        return new DimensionAverages(null, null, null, null);
    }
}
