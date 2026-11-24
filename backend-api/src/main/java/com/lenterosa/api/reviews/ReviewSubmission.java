package com.lenterosa.api.reviews;

import java.util.UUID;

/**
 * Opinión que una usuaria autenticada quiere publicar. {@code authorId} es el UID del token; nunca
 * se devuelve en ninguna respuesta.
 *
 * @param nightExperience nula si fue de día y eligió "No fui de noche"
 * @param visitPeriod {@code DAY} o {@code NIGHT}
 */
public record ReviewSubmission(
        UUID placeId,
        String authorId,
        Integer lighting,
        Integer womenPresence,
        Integer commercialVisibility,
        Integer nightExperience,
        String visitPeriod,
        String comment) {
}
