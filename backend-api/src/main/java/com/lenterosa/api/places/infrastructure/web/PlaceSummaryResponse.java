package com.lenterosa.api.places.infrastructure.web;

import com.lenterosa.api.places.application.NearbyPlace;
import com.lenterosa.api.places.domain.Place;
import java.util.UUID;

/** Lugar en una lista de resultados, con la distancia redondeada a metros enteros. */
record PlaceSummaryResponse(UUID id, String name, String category, String address, LocationResponse location,
        long distanceMeters) {

    static PlaceSummaryResponse from(NearbyPlace nearby) {
        Place place = nearby.place();
        return new PlaceSummaryResponse(place.id(), place.name(), place.category().name(), place.address(),
                LocationResponse.from(place.location()), Math.round(nearby.distanceMeters()));
    }
}
