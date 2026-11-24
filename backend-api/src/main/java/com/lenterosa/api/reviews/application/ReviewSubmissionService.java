package com.lenterosa.api.reviews.application;

import com.lenterosa.api.reviews.PublicReview;
import com.lenterosa.api.reviews.ReviewRateLimitedException;
import com.lenterosa.api.reviews.ReviewRejectedException;
import com.lenterosa.api.reviews.ReviewRejectedException.Reason;
import com.lenterosa.api.reviews.ReviewSubmission;
import com.lenterosa.api.reviews.ReviewSubmissions;
import com.lenterosa.api.reviews.domain.Comment;
import com.lenterosa.api.reviews.domain.ContactDataDetector;
import com.lenterosa.api.reviews.domain.DimensionScores;
import com.lenterosa.api.reviews.domain.Review;
import com.lenterosa.api.reviews.domain.VisitPeriod;
import com.lenterosa.api.shared.UuidV7;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publicar una opinión (RF-04), con los límites contra abuso aprobados (ADR 0007):
 *
 * <ul>
 *   <li>una opinión por autora, por lugar y por periodo (día o noche) cada 7 días;</li>
 *   <li>como máximo 20 opiniones por autora cada 24 horas;</li>
 *   <li>sin datos de contacto en el comentario.</li>
 * </ul>
 *
 * <p>Usa {@link Transactional} para que el bloqueo por autora, los conteos y la escritura sean atómicos.
 */
public class ReviewSubmissionService implements ReviewSubmissions {

    public static final Duration PER_PLACE_WINDOW = Duration.ofDays(7);
    public static final Duration DAILY_WINDOW = Duration.ofHours(24);
    public static final int DAILY_LIMIT = 20;

    private final ReviewRepository reviews;
    private final Clock clock;

    public ReviewSubmissionService(ReviewRepository reviews, Clock clock) {
        this.reviews = reviews;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PublicReview submit(ReviewSubmission submission) {
        Instant now = clock.instant();
        Review review = toReview(submission, now);
        if (review.comment() != null && ContactDataDetector.containsContactData(review.comment().text())) {
            throw new ReviewRejectedException(Reason.CONTACT_DATA,
                    "Por tu seguridad y la de otras personas, no incluyas datos de contacto ni enlaces.");
        }

        reviews.lockAuthor(submission.authorId());
        Optional<Instant> samePlaceAndPeriod = reviews.oldestReviewByAuthorSince(
                submission.authorId(), review.placeId(), review.visitPeriod(), now.minus(PER_PLACE_WINDOW));
        if (samePlaceAndPeriod.isPresent()) {
            throw new ReviewRateLimitedException(
                    "Ya opinaste sobre este lugar para este momento del día. Podrás volver a hacerlo en unos días.",
                    Duration.between(now, samePlaceAndPeriod.get().plus(PER_PLACE_WINDOW)));
        }
        List<Instant> lastDay = reviews.reviewTimesByAuthorSince(submission.authorId(), now.minus(DAILY_WINDOW));
        if (lastDay.size() >= DAILY_LIMIT) {
            // Se libera un cupo cuando la opinión número 20 contando desde la más reciente sale de la ventana.
            Instant freesUp = lastDay.get(lastDay.size() - DAILY_LIMIT).plus(DAILY_WINDOW);
            throw new ReviewRateLimitedException(
                    "Llegaste al máximo de opiniones por ahora. Inténtalo de nuevo más tarde.",
                    Duration.between(now, freesUp));
        }

        reviews.save(review, submission.authorId());
        return PublicReviews.from(review);
    }

    private static Review toReview(ReviewSubmission submission, Instant now) {
        if (submission.authorId() == null || submission.authorId().isBlank()) {
            throw new IllegalArgumentException("La autora es obligatoria");
        }
        if (submission.lighting() == null || submission.womenPresence() == null
                || submission.commercialVisibility() == null) {
            throw invalid("Califica iluminación, afluencia de mujeres y visibilidad comercial.");
        }
        VisitPeriod period;
        try {
            period = VisitPeriod.valueOf(String.valueOf(submission.visitPeriod()));
        } catch (IllegalArgumentException e) {
            throw invalid("Indica si fuiste de día o de noche.");
        }
        DimensionScores scores;
        try {
            scores = DimensionScores.of(submission.lighting(), submission.womenPresence(),
                    submission.commercialVisibility(), submission.nightExperience());
        } catch (IllegalArgumentException e) {
            throw invalid("Cada aspecto se califica de 1 a 5.");
        }
        Comment comment;
        try {
            comment = Comment.ofNullable(submission.comment());
        } catch (IllegalArgumentException e) {
            throw invalid("El comentario puede tener hasta " + Comment.MAX_LENGTH + " caracteres.");
        }
        if (period == VisitPeriod.NIGHT && !scores.hasNightExperience()) {
            throw invalid("Si fuiste de noche, califica también la experiencia nocturna.");
        }
        return new Review(UuidV7.next(), submission.placeId(), scores, period, comment, now);
    }

    private static ReviewRejectedException invalid(String message) {
        return new ReviewRejectedException(Reason.INVALID, message);
    }
}
