package com.lenterosa.api.reviews.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.lenterosa.api.PostgisTestcontainersConfiguration;
import com.lenterosa.api.reviews.application.RawReviewStats;
import com.lenterosa.api.reviews.application.ReviewRepository;
import com.lenterosa.api.reviews.domain.Comment;
import com.lenterosa.api.reviews.domain.DimensionScores;
import com.lenterosa.api.reviews.domain.Review;
import com.lenterosa.api.reviews.domain.VisitPeriod;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

// Cada prueba corre en una transacción que se revierte al terminar.
@SpringBootTest
@Import(PostgisTestcontainersConfiguration.class)
@Transactional
class JpaReviewRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-10-10T20:00:00Z");

    @Autowired
    ReviewRepository repository;

    @Autowired
    ReviewJpaRepository jpa;

    @Autowired
    JdbcTemplate jdbc;

    private UUID placeId;
    private UUID otherPlaceId;

    @BeforeEach
    void insertPlaces() {
        placeId = insertPlace();
        otherPlaceId = insertPlace();
    }

    @Test
    @DisplayName("Sin opiniones: cantidad 0, sin fecha y promedios nulos")
    void statsWithoutReviews() {
        RawReviewStats stats = repository.statsForPlace(placeId);

        assertThat(stats.reviewCount()).isZero();
        assertThat(stats.lastReviewAt()).isNull();
        assertThat(stats.lightingAverage()).isNull();
        assertThat(stats.nightExperienceAverage()).isNull();
    }

    @Test
    @DisplayName("El promedio nocturno ignora las opiniones de día sin experiencia nocturna")
    void nightAverageIgnoresMissingValues() {
        save(placeId, DimensionScores.of(4, 5, 3, null), VisitPeriod.DAY, T0);
        save(placeId, DimensionScores.of(2, 2, 2, 2), VisitPeriod.NIGHT, T0.plusSeconds(60));
        save(placeId, DimensionScores.of(3, 2, 4, 5), VisitPeriod.NIGHT, T0.plusSeconds(120));

        RawReviewStats stats = repository.statsForPlace(placeId);

        assertThat(stats.reviewCount()).isEqualTo(3);
        assertThat(stats.lastReviewAt()).isEqualTo(T0.plusSeconds(120));
        assertThat(stats.lightingAverage()).isCloseTo(3.0, within(1e-9));
        assertThat(stats.womenPresenceAverage()).isCloseTo(3.0, within(1e-9));
        assertThat(stats.commercialVisibilityAverage()).isCloseTo(3.0, within(1e-9));
        // (2 + 5) / 2 = 3.5. Si el nulo contara como 0, daría 2.33.
        assertThat(stats.nightExperienceAverage()).isCloseTo(3.5, within(1e-9));
    }

    @Test
    @DisplayName("Las opiniones de otro lugar no cuentan")
    void statsOnlyCountTheGivenPlace() {
        save(placeId, DimensionScores.of(5, 5, 5, 5), VisitPeriod.NIGHT, T0);
        save(otherPlaceId, DimensionScores.of(1, 1, 1, 1), VisitPeriod.NIGHT, T0);

        RawReviewStats stats = repository.statsForPlace(placeId);

        assertThat(stats.reviewCount()).isEqualTo(1);
        assertThat(stats.lightingAverage()).isCloseTo(5.0, within(1e-9));
    }

    @Test
    @DisplayName("Cada dimensión se guarda en su propia columna")
    void eachDimensionIsStoredInItsColumn() {
        Review review = save(placeId, DimensionScores.of(1, 2, 3, 4), VisitPeriod.NIGHT, T0);

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT lighting, women_presence, commercial_visibility, night_experience, visit_period, author_id
                FROM reviews WHERE id = ?
                """, review.id());

        // El driver devuelve smallint como Integer: se compara el valor numérico.
        assertThat(((Number) row.get("lighting")).intValue()).isEqualTo(1);
        assertThat(((Number) row.get("women_presence")).intValue()).isEqualTo(2);
        assertThat(((Number) row.get("commercial_visibility")).intValue()).isEqualTo(3);
        assertThat(((Number) row.get("night_experience")).intValue()).isEqualTo(4);
        assertThat(row.get("visit_period")).isEqualTo("NIGHT");
        assertThat(row.get("author_id")).isNull();
    }

    @Test
    @DisplayName("Una opinión de día sin experiencia nocturna se lee igual que se guardó")
    void dayReviewRoundTrip() {
        Review saved = save(placeId, DimensionScores.of(4, 4, 3, null), VisitPeriod.DAY, T0);

        Review read = repository.findByPlaceId(placeId, PageRequest.of(0, 10)).getContent().getFirst();

        assertThat(read).isEqualTo(saved);
        assertThat(read.scores().nightExperience()).isNull();
    }

    @Test
    @DisplayName("El listado va de la más reciente a la más antigua y se pagina")
    void listIsNewestFirstAndPaged() {
        Review oldest = save(placeId, DimensionScores.of(1, 1, 1, 1), VisitPeriod.NIGHT, T0);
        Review middle = save(placeId, DimensionScores.of(2, 2, 2, 2), VisitPeriod.NIGHT, T0.plusSeconds(60));
        Review newest = save(placeId, DimensionScores.of(3, 3, 3, 3), VisitPeriod.NIGHT, T0.plusSeconds(120));
        save(otherPlaceId, DimensionScores.of(5, 5, 5, 5), VisitPeriod.NIGHT, T0.plusSeconds(180));

        Page<Review> first = repository.findByPlaceId(placeId, PageRequest.of(0, 2));
        Page<Review> second = repository.findByPlaceId(placeId, PageRequest.of(1, 2));

        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getContent()).extracting(Review::id).containsExactly(newest.id(), middle.id());
        assertThat(second.getContent()).extracting(Review::id).containsExactly(oldest.id());
    }

    private Review save(UUID place, DimensionScores scores, VisitPeriod period, Instant createdAt) {
        Review review = new Review(UUID.randomUUID(), place, scores, period, Comment.ofNullable("Comentario de prueba"),
                createdAt);
        jpa.saveAndFlush(ReviewEntity.fromDomain(review));
        return review;
    }

    private UUID insertPlace() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO places (id, name, category, location)
                VALUES (?, 'Lugar de prueba', 'PARK', ST_SetSRID(ST_MakePoint(-77.2811, 1.2136), 4326)::geography)
                """, id);
        return id;
    }
}
