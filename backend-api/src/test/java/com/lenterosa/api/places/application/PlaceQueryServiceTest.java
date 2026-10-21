package com.lenterosa.api.places.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lenterosa.api.places.domain.GeoPoint;
import com.lenterosa.api.places.domain.Place;
import com.lenterosa.api.places.domain.PlaceCategory;
import com.lenterosa.api.reviews.DimensionAverages;
import com.lenterosa.api.reviews.PublicReview;
import com.lenterosa.api.reviews.ReviewQueries;
import com.lenterosa.api.reviews.ReviewStats;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class PlaceQueryServiceTest {

    private static final GeoPoint PASTO = new GeoPoint(1.2136, -77.2811);
    private static final Place PARK = new Place(UUID.randomUUID(), "Parque de prueba", PlaceCategory.PARK, null, PASTO,
            Instant.parse("2026-10-01T00:00:00Z"));
    private static final ReviewStats PARK_STATS = new ReviewStats(2, Instant.parse("2026-10-18T22:10:00Z"),
            new DimensionAverages(3.0, 3.5, 2.5, 2.0));

    private final PlaceQueryService service = new PlaceQueryService(
            new FakePlaces(Map.of(PARK.id(), PARK)), new FakeReviews(Map.of(PARK.id(), PARK_STATS)));

    @Test
    @DisplayName("El detalle combina el lugar con el resumen de sus opiniones")
    void detailCombinesPlaceAndStats() {
        PlaceDetail detail = service.getDetail(PARK.id());

        assertThat(detail.place()).isEqualTo(PARK);
        assertThat(detail.reviewStats()).isEqualTo(PARK_STATS);
    }

    @Test
    @DisplayName("El detalle de un lugar inexistente lanza PlaceNotFoundException")
    void detailOfUnknownPlaceFails() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.getDetail(unknown))
                .isInstanceOf(PlaceNotFoundException.class)
                .extracting("placeId").isEqualTo(unknown);
    }

    @Test
    @DisplayName("Las opiniones de un lugar inexistente lanzan PlaceNotFoundException")
    void reviewsOfUnknownPlaceFail() {
        assertThatThrownBy(() -> service.listReviews(UUID.randomUUID(), PageRequest.of(0, 20)))
                .isInstanceOf(PlaceNotFoundException.class);
    }

    @Test
    @DisplayName("Una página de más de 50 elementos se rechaza")
    void rejectsPageSizeAboveLimit() {
        NearbySearch search = new NearbySearch(PASTO, NearbySearch.DEFAULT_RADIUS_METERS, null);

        assertThatIllegalArgumentException().isThrownBy(() -> service.searchNearby(search, PageRequest.of(0, 51)));
        assertThat(service.searchNearby(search, PageRequest.of(0, 50)).getContent()).hasSize(1);
    }

    @Test
    @DisplayName("El radio debe estar entre 1 y 5000 metros")
    void radiusMustBeWithinLimits() {
        assertThatIllegalArgumentException().isThrownBy(() -> new NearbySearch(PASTO, 0, null));
        assertThatIllegalArgumentException().isThrownBy(() -> new NearbySearch(PASTO, 5_001, null));
        assertThat(new NearbySearch(PASTO, 5_000, null).radiusMeters()).isEqualTo(5_000);
    }

    @Test
    @DisplayName("Una búsqueda por nombre en blanco no filtra")
    void blankNameQueryMeansNoFilter() {
        assertThat(new NearbySearch(PASTO, 1_000, "   ").nameQuery()).isNull();
        assertThat(new NearbySearch(PASTO, 1_000, "  parque ").nameQuery()).isEqualTo("parque");
    }

    private record FakePlaces(Map<UUID, Place> places) implements PlaceRepository {

        @Override
        public Optional<Place> findById(UUID id) {
            return Optional.ofNullable(places.get(id));
        }

        @Override
        public boolean existsById(UUID id) {
            return places.containsKey(id);
        }

        @Override
        public Page<NearbyPlace> findNearby(NearbySearch search, Pageable pageable) {
            List<NearbyPlace> all = places.values().stream().map(place -> new NearbyPlace(place, 0)).toList();
            return new PageImpl<>(all, pageable, all.size());
        }
    }

    private record FakeReviews(Map<UUID, ReviewStats> stats) implements ReviewQueries {

        @Override
        public ReviewStats statsForPlace(UUID placeId) {
            return stats.getOrDefault(placeId, ReviewStats.empty());
        }

        @Override
        public Page<PublicReview> listForPlace(UUID placeId, Pageable pageable) {
            return Page.empty(pageable);
        }
    }
}
