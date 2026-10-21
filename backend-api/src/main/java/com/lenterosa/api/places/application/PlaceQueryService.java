package com.lenterosa.api.places.application;

import com.lenterosa.api.reviews.PublicReview;
import com.lenterosa.api.reviews.ReviewQueries;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Casos de uso de consulta como invitada: buscar lugares cercanos, ver el detalle de un lugar
 * y listar sus opiniones anónimas.
 *
 * <p>Sin anotaciones de Spring: la capa de infraestructura lo registra como bean.
 */
public class PlaceQueryService {

    public static final int MAX_PAGE_SIZE = 50;

    private final PlaceRepository places;
    private final ReviewQueries reviews;

    public PlaceQueryService(PlaceRepository places, ReviewQueries reviews) {
        this.places = places;
        this.reviews = reviews;
    }

    public Page<NearbyPlace> searchNearby(NearbySearch search, Pageable pageable) {
        requireValidPage(pageable);
        return places.findNearby(search, pageable);
    }

    public PlaceDetail getDetail(UUID placeId) {
        return places.findById(placeId)
                .map(place -> new PlaceDetail(place, reviews.statsForPlace(placeId)))
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
