package com.lenterosa.api.places.application;

import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.comfortindex.ComfortIndexQueries;
import com.lenterosa.api.places.ReviewContributed;
import com.lenterosa.api.reviews.PublicReview;
import com.lenterosa.api.reviews.ReviewSubmission;
import com.lenterosa.api.reviews.ReviewSubmissions;
import java.util.UUID;

/**
 * Aportar una opinión sobre un lugar (RF-04). Vive en {@code places} porque debe responder 404 si el
 * lugar no existe, igual que el listado de opiniones (ADR 0002).
 *
 * <p>Después de publicar, avisa con el índice de antes y el de después (RF-07). Las alertas
 * reaccionan a cada opinión, sin procesos programados.
 *
 * <p>Sin anotaciones de Spring: la capa de infraestructura lo registra como bean.
 */
public class ContributeReviewService {

    private final PlaceRepository places;
    private final ReviewSubmissions reviews;
    private final ComfortIndexQueries comfort;
    private final ContributionEvents events;

    public ContributeReviewService(PlaceRepository places, ReviewSubmissions reviews, ComfortIndexQueries comfort,
            ContributionEvents events) {
        this.places = places;
        this.reviews = reviews;
        this.comfort = comfort;
        this.events = events;
    }

    public PublicReview contribute(ReviewSubmission submission) {
        UUID placeId = submission.placeId();
        if (!places.existsById(placeId)) {
            throw new PlaceNotFoundException(placeId);
        }
        ComfortIndex before = comfort.forPlace(placeId);
        PublicReview review = reviews.submit(submission);
        ComfortIndex after = comfort.forPlace(placeId);
        events.published(new ReviewContributed(placeId, submission.authorId(), before, after));
        return review;
    }
}
