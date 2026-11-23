package com.lenterosa.api.places.application;

import com.lenterosa.api.comfortindex.ComfortIndex;

/** Lugar encontrado en una búsqueda, con su distancia y su Índice de Confort. */
public record PlaceSearchResult(NearbyPlace nearby, ComfortIndex comfort) {
}
