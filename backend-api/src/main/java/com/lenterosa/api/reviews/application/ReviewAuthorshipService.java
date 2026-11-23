package com.lenterosa.api.reviews.application;

import com.lenterosa.api.reviews.ReviewAuthorship;

/**
 * Gestión de la autoría de las opiniones.
 *
 * <p>Sin anotaciones de Spring: la capa de infraestructura lo registra como bean.
 */
public class ReviewAuthorshipService implements ReviewAuthorship {

    private final ReviewRepository reviews;

    public ReviewAuthorshipService(ReviewRepository reviews) {
        this.reviews = reviews;
    }

    @Override
    public int anonymizeReviewsBy(String authorId) {
        if (authorId == null || authorId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la autora es obligatorio");
        }
        return reviews.clearAuthor(authorId);
    }
}
