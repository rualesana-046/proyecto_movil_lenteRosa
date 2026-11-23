package com.lenterosa.api.places.application;

import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.places.domain.Place;
import com.lenterosa.api.reviews.ReviewStats;

/** Lugar con el resumen de sus opiniones y su Índice de Confort (RF-03). */
public record PlaceDetail(Place place, ReviewStats reviewStats, ComfortIndex comfort) {
}
