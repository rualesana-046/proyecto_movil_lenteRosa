package com.lenterosa.api.reviews;

import java.util.UUID;

/**
 * Sumas de las calificaciones de un lugar, ponderadas por antigüedad con decaimiento exponencial
 * ({@code peso = 0,5 ^ (edad en días / vida media)}). Es una estadística genérica: no contiene la
 * fórmula de ningún índice. No incluye comentarios ni datos de la autora.
 *
 * @param totalWeight suma de los pesos; es también la evidencia de las dimensiones obligatorias
 * @param nightExperienceWeight suma de los pesos de las opiniones que califican la experiencia nocturna
 * @param reviewMeanWeightedSum suma ponderada del promedio de cada opinión (solo sus dimensiones presentes)
 * @param reviewMeanSquaredWeightedSum suma ponderada del cuadrado de ese promedio
 */
public record DecayedRatingSums(
        UUID placeId,
        long reviewCount,
        double totalWeight,
        double lightingWeightedSum,
        double womenPresenceWeightedSum,
        double commercialVisibilityWeightedSum,
        double nightExperienceWeight,
        double nightExperienceWeightedSum,
        double reviewMeanWeightedSum,
        double reviewMeanSquaredWeightedSum) {
}
