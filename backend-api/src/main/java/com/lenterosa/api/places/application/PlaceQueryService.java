package com.lenterosa.api.places.application;

import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.comfortindex.ComfortIndexQueries;
import com.lenterosa.api.reviews.PublicReview;
import com.lenterosa.api.reviews.ReviewQueries;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de consulta como invitada: buscar lugares cercanos, ver el detalle de un lugar
 * con su Índice de Confort y listar sus opiniones anónimas.
 *
 * <p>Sin anotaciones de Spring: la capa de infraestructura lo registra como bean.
 */
public class PlaceQueryService {

    public static final int MAX_PAGE_SIZE = 50;

    private final PlaceRepository places;
    private final ReviewQueries reviews;
    private final ComfortIndexQueries comfort;

    public PlaceQueryService(PlaceRepository places, ReviewQueries reviews, ComfortIndexQueries comfort) {
        this.places = places;
        this.reviews = reviews;
        this.comfort = comfort;
    }

    /** Lugares cercanos con su índice; el índice de toda la página se calcula con una sola consulta. */
    public Page<PlaceSearchResult> searchNearby(NearbySearch search, Pageable pageable) {
        requireValidPage(pageable);
        Page<NearbyPlace> page = places.findNearby(search, pageable);
        List<UUID> ids = page.getContent().stream().map(nearby -> nearby.place().id()).toList();
        Map<UUID, ComfortIndex> comfortByPlace = ids.isEmpty() ? Map.of() : comfort.forPlaces(ids);
        return page.map(nearby -> new PlaceSearchResult(nearby, comfortByPlace.get(nearby.place().id())));
    }

    public PlaceDetail getDetail(UUID placeId) {
        return places.findById(placeId)
                .map(place -> new PlaceDetail(place, reviews.statsForPlace(placeId), comfort.forPlace(placeId)))
                .orElseThrow(() -> new PlaceNotFoundException(placeId));
    }

    public Page<PublicReview> listReviews(UUID placeId, Pageable pageable) {
        requireValidPage(pageable);
        if (!places.existsById(placeId)) {
            throw new PlaceNotFoundException(placeId);
        }
        return reviews.listForPlace(placeId, pageable);
    }

    private static void requireValidPage(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("El tamaño de página máximo es " + MAX_PAGE_SIZE);
        }
    }
}
