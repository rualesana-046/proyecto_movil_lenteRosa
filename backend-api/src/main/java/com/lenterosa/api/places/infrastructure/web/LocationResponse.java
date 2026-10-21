package com.lenterosa.api.places.infrastructure.web;

import com.lenterosa.api.places.domain.GeoPoint;

/** Ubicación de un lugar en el JSON: nombres explícitos para no confundir el orden. */
record LocationResponse(double lat, double lng) {

    static LocationResponse from(GeoPoint point) {
        return new LocationResponse(point.latitude(), point.longitude());
    }
}
