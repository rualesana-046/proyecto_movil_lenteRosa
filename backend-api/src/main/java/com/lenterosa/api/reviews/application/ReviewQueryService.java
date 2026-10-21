package com.lenterosa.api.reviews.application;

import com.lenterosa.api.reviews.DimensionAverages;
import com.lenterosa.api.reviews.PublicReview;
import com.lenterosa.api.reviews.ReviewQueries;
import com.lenterosa.api.reviews.ReviewStats;
import com.lenterosa.api.reviews.domain.Comment;
import com.lenterosa.api.reviews.domain.Rating;
import com.lenterosa.api.reviews.domain.Review;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de lectura de opiniones: estadísticas por lugar y listado anónimo.
 *
 * <p>Sin anotaciones de Spring: la capa de infraestructura lo registra como bean.
 */
public class ReviewQueryService implements ReviewQueries {

    private final ReviewRepository reviews;

    public ReviewQueryService(ReviewRepository reviews) {
        this.reviews = reviews;
    }

    @Override
    public ReviewStats statsForPlace(UUID placeId) {
        RawReviewStats raw = reviews.statsForPlace(placeId);
        if (raw.reviewCount() == 0) {
            return ReviewStats.empty();
        }
        DimensionAverages averages = new DimensionAverages(
                roundToOneDecimal(raw.lightingAverage()),
                roundToOneDecimal(raw.womenPresenceAverage()),
                roundToOneDecimal(raw.commercialVisibilityAverage()),
                roundToOneDecimal(raw.nightExperienceAverage()));
        return new ReviewStats(raw.reviewCount(), raw.lastReviewAt(), averages);
    }

    @Override
    public Page<PublicReview> listForPlace(UUID placeId, Pageable pageable) {
        return reviews.findByPlaceId(placeId, pageable).map(ReviewQueryService::toPublic);
    }

    private static PublicReview toPublic(Review review) {
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

    // Redondeo "half up" sobre el decimal exacto: 3.25 → 3.3 y 3.35 → 3.4.
    private static Double roundToOneDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
