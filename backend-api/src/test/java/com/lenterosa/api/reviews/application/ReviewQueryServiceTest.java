package com.lenterosa.api.reviews.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.lenterosa.api.reviews.PublicReview;
import com.lenterosa.api.reviews.ReviewStats;
import com.lenterosa.api.reviews.domain.Comment;
import com.lenterosa.api.reviews.domain.DimensionScores;
import com.lenterosa.api.reviews.domain.Review;
import com.lenterosa.api.reviews.domain.VisitPeriod;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class ReviewQueryServiceTest {

    private static final UUID PLACE_ID = UUID.randomUUID();
    private static final Instant LAST = Instant.parse("2026-10-18T22:10:00Z");

    @Test
    @DisplayName("Sin opiniones, la cantidad es 0 y todos los promedios son nulos")
    void noReviewsGivesNullAverages() {
        ReviewQueryService service = new ReviewQueryService(
                new FakeRepository(new RawReviewStats(0, null, null, null, null, null), List.of()));

        ReviewStats stats = service.statsForPlace(PLACE_ID);

        assertThat(stats.reviewCount()).isZero();
        assertThat(stats.lastReviewAt()).isNull();
        assertThat(stats.averages().lighting()).isNull();
        assertThat(stats.averages().womenPresence()).isNull();
        assertThat(stats.averages().commercialVisibility()).isNull();
        assertThat(stats.averages().nightExperience()).isNull();
    }

    @Test
    @DisplayName("Los promedios se redondean a un decimal (half up)")
    void roundsAveragesToOneDecimal() {
        ReviewQueryService service = new ReviewQueryService(
                new FakeRepository(new RawReviewStats(3, LAST, 3.6666, 3.25, 2.3333, 3.35), List.of()));

        ReviewStats stats = service.statsForPlace(PLACE_ID);

        assertThat(stats.reviewCount()).isEqualTo(3);
        assertThat(stats.lastReviewAt()).isEqualTo(LAST);
        assertThat(stats.averages().lighting()).isEqualTo(3.7);
        assertThat(stats.averages().womenPresence()).isEqualTo(3.3);
        assertThat(stats.averages().commercialVisibility()).isEqualTo(2.3);
        assertThat(stats.averages().nightExperience()).isEqualTo(3.4);
    }

    @Test
    @DisplayName("Si nadie calificó la experiencia nocturna, su promedio queda nulo")
    void nightAverageStaysNullWhenNobodyRatedIt() {
        ReviewQueryService service = new ReviewQueryService(
                new FakeRepository(new RawReviewStats(2, LAST, 4.5, 4.0, 3.0, null), List.of()));

        ReviewStats stats = service.statsForPlace(PLACE_ID);

        assertThat(stats.averages().lighting()).isEqualTo(4.5);
        assertThat(stats.averages().nightExperience()).isNull();
    }

    @Test
    @DisplayName("El listado público conserva cada dimensión, el periodo y el comentario")
    void listMapsEveryField() {
        Review day = new Review(UUID.randomUUID(), PLACE_ID, DimensionScores.of(1, 2, 3, null), VisitPeriod.DAY,
                Comment.ofNullable("Comentario de prueba"), LAST);
        Review night = new Review(UUID.randomUUID(), PLACE_ID, DimensionScores.of(5, 4, 3, 2), VisitPeriod.NIGHT,
                null, LAST.minusSeconds(60));
        ReviewQueryService service = new ReviewQueryService(
                new FakeRepository(new RawReviewStats(2, LAST, null, null, null, null), List.of(day, night)));

        List<PublicReview> page = service.listForPlace(PLACE_ID, PageRequest.of(0, 20)).getContent();

        assertThat(page).hasSize(2);
        PublicReview first = page.get(0);
        assertThat(first.lighting()).isEqualTo(1);
        assertThat(first.womenPresence()).isEqualTo(2);
        assertThat(first.commercialVisibility()).isEqualTo(3);
        assertThat(first.nightExperience()).isNull();
        assertThat(first.visitPeriod()).isEqualTo("DAY");
        assertThat(first.comment()).isEqualTo("Comentario de prueba");
        PublicReview second = page.get(1);
        assertThat(second.nightExperience()).isEqualTo(2);
        assertThat(second.visitPeriod()).isEqualTo("NIGHT");
        assertThat(second.comment()).isNull();
    }

    @Test
    @DisplayName("La opinión pública no tiene ningún campo de autora")
    void publicReviewHasNoAuthorField() {
        List<String> fields = Arrays.stream(PublicReview.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(fields).noneMatch(name -> name.toLowerCase().contains("author"));
    }

    private record FakeRepository(RawReviewStats stats, List<Review> reviews) implements ReviewRepository {

        @Override
        public Page<Review> findByPlaceId(UUID placeId, Pageable pageable) {
            return new PageImpl<>(reviews, pageable, reviews.size());
        }

        @Override
        public RawReviewStats statsForPlace(UUID placeId) {
            return stats;
        }
    }
}
