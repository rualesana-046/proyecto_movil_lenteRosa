package com.lenterosa.api.reviews.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ReviewJpaRepository extends JpaRepository<ReviewEntity, UUID> {

    Page<ReviewEntity> findByPlaceId(UUID placeId, Pageable pageable);

    // avg() ignora los nulos: el promedio nocturno sale solo de las opiniones que lo califican.
    @Query("""
            select count(r) as reviewCount,
                   max(r.createdAt) as lastReviewAt,
                   avg(r.lighting) as lightingAverage,
                   avg(r.womenPresence) as womenPresenceAverage,
                   avg(r.commercialVisibility) as commercialVisibilityAverage,
                   avg(r.nightExperience) as nightExperienceAverage
            from ReviewEntity r
            where r.placeId = :placeId
            """)
    StatsRow statsForPlace(@Param("placeId") UUID placeId);

    interface StatsRow {
        long getReviewCount();

        Instant getLastReviewAt();

        Double getLightingAverage();

        Double getWomenPresenceAverage();

        Double getCommercialVisibilityAverage();

        Double getNightExperienceAverage();
    }
}
