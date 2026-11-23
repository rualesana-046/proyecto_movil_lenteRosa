package com.lenterosa.api.reviews.infrastructure.persistence;

import com.lenterosa.api.reviews.DecayedRatingSums;
import com.lenterosa.api.reviews.application.RawReviewStats;
import com.lenterosa.api.reviews.application.ReviewRepository;
import com.lenterosa.api.reviews.domain.Review;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador del puerto {@link ReviewRepository}: JPA para leer opiniones y SQL nativo para las
 * sumas ponderadas por antigüedad, que se calculan en la base y devuelven una fila por lugar.
 */
@Repository
@Transactional(readOnly = true)
class JpaReviewRepository implements ReviewRepository {

    // Más reciente primero; el id desempata para que la paginación sea estable.
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    // Peso = 0,5 ^ (edad en días / vida media); una fecha futura cuenta como edad 0.
    // El promedio de cada opinión usa solo sus dimensiones presentes (3 sin experiencia nocturna, 4 con ella).
    private static final String DECAYED_SUMS_SQL = """
            WITH weighted AS (
                SELECT place_id,
                       power(0.5, GREATEST(EXTRACT(EPOCH FROM (CAST(:now AS timestamptz) - created_at)), 0)
                                  / 86400.0 / :halfLifeDays) AS weight,
                       lighting, women_presence, commercial_visibility, night_experience,
                       (lighting + women_presence + commercial_visibility + COALESCE(night_experience, 0))::float8
                           / CASE WHEN night_experience IS NULL THEN 3 ELSE 4 END AS review_mean
                FROM reviews
                WHERE place_id IN (:placeIds)
            )
            SELECT place_id,
                   count(*)                                                    AS review_count,
                   sum(weight)                                                 AS total_weight,
                   sum(weight * lighting)                                      AS lighting_sum,
                   sum(weight * women_presence)                                AS women_presence_sum,
                   sum(weight * commercial_visibility)                         AS commercial_visibility_sum,
                   COALESCE(sum(weight) FILTER (WHERE night_experience IS NOT NULL), 0) AS night_weight,
                   COALESCE(sum(weight * night_experience), 0)                 AS night_sum,
                   sum(weight * review_mean)                                   AS mean_sum,
                   sum(weight * review_mean * review_mean)                     AS mean_squared_sum
            FROM weighted
            GROUP BY place_id
            """;

    private final ReviewJpaRepository jpa;
    private final JdbcClient jdbc;

    JpaReviewRepository(ReviewJpaRepository jpa, JdbcClient jdbc) {
        this.jpa = jpa;
        this.jdbc = jdbc;
    }

    @Override
    public Page<Review> findByPlaceId(UUID placeId, Pageable pageable) {
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
        return jpa.findByPlaceId(placeId, newestFirst).map(ReviewEntity::toDomain);
    }

    @Override
    public List<DecayedRatingSums> decayedRatingSums(Collection<UUID> placeIds, Instant now, double halfLifeDays) {
        return jdbc.sql(DECAYED_SUMS_SQL)
                .param("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                .param("halfLifeDays", halfLifeDays)
                .param("placeIds", placeIds)
                .query((rs, rowNum) -> new DecayedRatingSums(
                        rs.getObject("place_id", UUID.class),
                        rs.getLong("review_count"),
                        rs.getDouble("total_weight"),
                        rs.getDouble("lighting_sum"),
                        rs.getDouble("women_presence_sum"),
                        rs.getDouble("commercial_visibility_sum"),
                        rs.getDouble("night_weight"),
                        rs.getDouble("night_sum"),
                        rs.getDouble("mean_sum"),
                        rs.getDouble("mean_squared_sum")))
                .list();
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
