package com.lenterosa.api.comfortindex;

/**
 * Promedio bayesiano de cada dimensión, en escala 1 a 5, sin redondear.
 *
 * @param nightExperience nulo cuando ninguna opinión califica la experiencia nocturna
 */
public record DimensionComfort(Double lighting, Double womenPresence, Double commercialVisibility, Double nightExperience) {

    public static DimensionComfort none() {
        return new DimensionComfort(null, null, null, null);
    }
}
