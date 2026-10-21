package com.lenterosa.api.places.application;

import com.lenterosa.api.places.domain.Place;
import com.lenterosa.api.reviews.ReviewStats;

/** Lugar con el resumen de sus opiniones. El Índice de Confort se agrega el día 3. */
public record PlaceDetail(Place place, ReviewStats reviewStats) {
}
