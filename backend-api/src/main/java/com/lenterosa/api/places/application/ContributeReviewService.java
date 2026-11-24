package com.lenterosa.api.places.application;

import com.lenterosa.api.reviews.PublicReview;
import com.lenterosa.api.reviews.ReviewSubmission;
import com.lenterosa.api.reviews.ReviewSubmissions;
import java.util.UUID;

/**
 * Aportar una opinión sobre un lugar (RF-04). Vive en {@code places} porque debe responder 404 si el
 * lugar no existe, igual que el listado de opiniones (ADR 0002).
 *
 * <p>Sin anotaciones de Spring: la capa de infraestructura lo registra como bean.
 */
public class ContributeReviewService {

    private final PlaceRepository places;
    private final ReviewSubmissions reviews;

    public ContributeReviewService(PlaceRepository places, ReviewSubmissions reviews) {
        this.places = places;
        this.reviews = reviews;
    }

    public PublicReview contribute(ReviewSubmission submission) {
        UUID placeId = submission.placeId();
        if (!places.existsById(placeId)) {
            throw new PlaceNotFoundException(placeId);
        }
        return reviews.submit(submission);
    }
}
