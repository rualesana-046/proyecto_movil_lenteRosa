package com.lenterosa.api.places.domain;

/**
 * Punto geográfico WGS 84 (SRID 4326), en grados decimales.
 *
 * <p>Se nombra siempre latitud y longitud de forma explícita: PostGIS y JTS usan el orden
 * (x = longitud, y = latitud), y confundirlo dejaría a Pasto en otro lugar del planeta.
 */
public record GeoPoint(double latitude, double longitude) {

    public GeoPoint {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("La latitud debe estar entre -90 y 90: " + latitude);
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("La longitud debe estar entre -180 y 180: " + longitude);
        }
    }
}
