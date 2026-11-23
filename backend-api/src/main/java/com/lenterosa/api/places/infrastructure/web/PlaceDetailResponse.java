package com.lenterosa.api.places.infrastructure.web;

import com.lenterosa.api.places.application.PlaceDetail;
import com.lenterosa.api.places.domain.Place;
import com.lenterosa.api.reviews.DimensionAverages;
import com.lenterosa.api.reviews.ReviewStats;
import java.time.Instant;
import java.util.UUID;

/**
 * Detalle de un lugar. {@code averages} son los promedios crudos (nulos cuando no hay datos; la app
 * muestra "Sin datos") y {@code comfort} es el Índice de Confort (RF-03).
 */
record PlaceDetailResponse(UUID id, String name, String category, String address, LocationResponse location,
        long reviewCount, Instant lastReviewAt, DimensionAverages averages, ComfortResponse comfort) {

    static PlaceDetailResponse from(PlaceDetail detail) {
        Place place = detail.place();
        ReviewStats stats = detail.reviewStats();
        return new PlaceDetailResponse(place.id(), place.name(), place.category().name(), place.address(),
                LocationResponse.from(place.location()), stats.reviewCount(), stats.lastReviewAt(), stats.averages(),
                ComfortResponse.from(detail.comfort()));
    }
}
