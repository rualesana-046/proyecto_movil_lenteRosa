package com.lenterosa.api.reviews.application;

import com.lenterosa.api.reviews.PublicReview;
import com.lenterosa.api.reviews.domain.Comment;
import com.lenterosa.api.reviews.domain.Rating;
import com.lenterosa.api.reviews.domain.Review;

/** Convierte una opinión a la forma pública: sin ningún dato de la autora. */
final class PublicReviews {

    private PublicReviews() {
    }

    static PublicReview from(Review review) {
        Rating night = review.scores().nightExperience();
        Comment comment = review.comment();
        return new PublicReview(
                review.id(),
                review.scores().lighting().value(),
                review.scores().womenPresence().value(),
                review.scores().commercialVisibility().value(),
                night == null ? null : night.value(),
                review.visitPeriod().name(),
                comment == null ? null : comment.text(),
                review.createdAt());
    }
}
