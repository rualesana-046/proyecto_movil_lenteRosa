package com.lenterosa.api.reviews;

import java.time.Instant;
import java.util.UUID;

/**
 * Calificaciones de una opinión, sin comentario ni datos de la autora: lo mínimo para calcular
 * índices sobre las opiniones de un lugar.
 *
 * @param nightExperience nula cuando fue una visita de día y no se calificó ("No fui de noche")
 */
public record ReviewRatings(
        UUID placeId,
        int lighting,
        int womenPresence,
        int commercialVisibility,
        Integer nightExperience,
        Instant createdAt) {
}
