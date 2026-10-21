package com.lenterosa.api.places.application;

import com.lenterosa.api.places.domain.Place;

/** Lugar encontrado en una búsqueda, con su distancia en metros al centro de la búsqueda. */
public record NearbyPlace(Place place, double distanceMeters) {
}
