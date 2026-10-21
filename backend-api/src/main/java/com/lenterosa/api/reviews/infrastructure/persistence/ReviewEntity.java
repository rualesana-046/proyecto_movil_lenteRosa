package com.lenterosa.api.reviews.infrastructure.persistence;

import com.lenterosa.api.reviews.domain.Comment;
import com.lenterosa.api.reviews.domain.DimensionScores;
import com.lenterosa.api.reviews.domain.Rating;
import com.lenterosa.api.reviews.domain.Review;
import com.lenterosa.api.reviews.domain.VisitPeriod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Fila de la tabla {@code reviews}. {@code author_id} solo existe aquí: el dominio y la API
 * nunca lo reciben, para que las opiniones salgan siempre anónimas.
 */
@Entity
@Table(name = "reviews")
class ReviewEntity {

    @Id
    private UUID id;

    @Column(name = "place_id", nullable = false)
    private UUID placeId;

    @Column(name = "author_id")
    private String authorId;

    @Column(name = "lighting", nullable = false)
    private short lighting;

    @Column(name = "women_presence", nullable = false)
    private short womenPresence;

    @Column(name = "commercial_visibility", nullable = false)
    private short commercialVisibility;

    @Column(name = "night_experience")
    private Short nightExperience;

    @Column(name = "visit_period", nullable = false)
    private String visitPeriod;

    @Column(name = "comment")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReviewEntity() {
        // Requerido por JPA.
    }

    static ReviewEntity fromDomain(Review review) {
        ReviewEntity entity = new ReviewEntity();
        entity.id = review.id();
        entity.placeId = review.placeId();
        entity.lighting = (short) review.scores().lighting().value();
        entity.womenPresence = (short) review.scores().womenPresence().value();
        entity.commercialVisibility = (short) review.scores().commercialVisibility().value();
        Rating night = review.scores().nightExperience();
        entity.nightExperience = night == null ? null : (short) night.value();
        entity.visitPeriod = review.visitPeriod().name();
        entity.comment = review.comment() == null ? null : review.comment().text();
        entity.createdAt = review.createdAt();
        return entity;
    }

    Review toDomain() {
        DimensionScores scores = DimensionScores.of(
                lighting, womenPresence, commercialVisibility,
                nightExperience == null ? null : nightExperience.intValue());
        return new Review(id, placeId, scores, VisitPeriod.valueOf(visitPeriod), Comment.ofNullable(comment), createdAt);
    }
}
