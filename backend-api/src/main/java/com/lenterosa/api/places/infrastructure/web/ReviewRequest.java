package com.lenterosa.api.places.infrastructure.web;

/**
 * Cuerpo de {@code POST /api/v1/places/{id}/reviews}. Las reglas (1 a 5, experiencia nocturna
 * obligatoria de noche, comentario sin datos de contacto) las valida el módulo de opiniones.
 *
 * @param nightExperience nula si fue de día y eligió "No fui de noche"
 * @param visitPeriod {@code DAY} o {@code NIGHT}
 */
record ReviewRequest(
        Integer lighting,
        Integer womenPresence,
        Integer commercialVisibility,
        Integer nightExperience,
        String visitPeriod,
        String comment) {
}
