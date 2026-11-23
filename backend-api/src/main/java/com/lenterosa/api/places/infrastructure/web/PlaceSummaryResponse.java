package com.lenterosa.api.places.infrastructure.web;

import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.places.application.PlaceSearchResult;
import com.lenterosa.api.places.domain.Place;
import java.util.UUID;

/**
 * Lugar en una lista de resultados, con la distancia redondeada a metros enteros y el Índice de
 * Confort resumido ({@code comfortIndex} nulo si no hay opiniones).
 */
record PlaceSummaryResponse(UUID id, String name, String category, String address, LocationResponse location,
        long distanceMeters, Integer comfortIndex, String comfortLabel) {

    static PlaceSummaryResponse from(PlaceSearchResult result) {
        Place place = result.nearby().place();
        ComfortIndex comfort = result.comfort();
        return new PlaceSummaryResponse(place.id(), place.name(), place.category().name(), place.address(),
                LocationResponse.from(place.location()), Math.round(result.nearby().distanceMeters()),
                comfort.index(), comfort.label().name());
    }
}
