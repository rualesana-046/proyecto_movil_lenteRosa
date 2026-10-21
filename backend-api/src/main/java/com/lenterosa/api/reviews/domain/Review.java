package com.lenterosa.api.reviews.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Opinión sobre un lugar. No tiene autora: la identidad se guarda solo en la base y nunca
 * sale del módulo, así las opiniones se muestran siempre anónimas.
 *
 * <p>Regla: en una visita de noche la experiencia nocturna es obligatoria; de día es opcional.
 */
public record Review(UUID id, UUID placeId, DimensionScores scores, VisitPeriod visitPeriod, Comment comment, Instant createdAt) {

    public Review {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(placeId, "placeId");
        Objects.requireNonNull(scores, "scores");
        Objects.requireNonNull(visitPeriod, "visitPeriod");
        Objects.requireNonNull(createdAt, "createdAt");
        if (visitPeriod == VisitPeriod.NIGHT && !scores.hasNightExperience()) {
            throw new IllegalArgumentException("En una visita de noche la experiencia nocturna es obligatoria");
        }
    }
}
