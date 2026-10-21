package com.lenterosa.api.reviews.infrastructure.persistence;

import com.lenterosa.api.reviews.application.RawReviewStats;
import com.lenterosa.api.reviews.application.ReviewRepository;
import com.lenterosa.api.reviews.domain.Review;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JPA del puerto {@link ReviewRepository}. */
@Repository
@Transactional(readOnly = true)
class JpaReviewRepository implements ReviewRepository {

    // Más reciente primero; el id desempata para que la paginación sea estable.
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final ReviewJpaRepository jpa;

    JpaReviewRepository(ReviewJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Page<Review> findByPlaceId(UUID placeId, Pageable pageable) {
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
        return jpa.findByPlaceId(placeId, newestFirst).map(ReviewEntity::toDomain);
    }

    @Override
    public RawReviewStats statsForPlace(UUID placeId) {
        ReviewJpaRepository.StatsRow row = jpa.statsForPlace(placeId);
        return new RawReviewStats(
                row.getReviewCount(),
                row.getLastReviewAt(),
                row.getLightingAverage(),
                row.getWomenPresenceAverage(),
                row.getCommercialVisibilityAverage(),
                row.getNightExperienceAverage());
    }
}
