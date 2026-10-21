package com.lenterosa.api.reviews;

import java.time.Instant;
import java.util.UUID;

/**
 * Opinión tal como se muestra a cualquier persona: sin ningún dato de la autora.
 *
 * @param nightExperience nula cuando fue una visita de día y no se calificó ("No fui de noche")
 * @param visitPeriod {@code DAY} o {@code NIGHT}
 * @param comment nulo si no hay comentario
 */
public record PublicReview(
        UUID id,
        int lighting,
        int womenPresence,
        int commercialVisibility,
        Integer nightExperience,
        String visitPeriod,
        String comment,
        Instant createdAt) {
}
