package com.lenterosa.api.places.application;

import com.lenterosa.api.places.domain.GeoPoint;
import java.util.Objects;

/**
 * Criterios de búsqueda de lugares cercanos.
 *
 * <p>El centro es la ubicación que la persona comparte para esta consulta: se usa solo para
 * calcular y no se guarda ni se registra en los logs.
 *
 * @param radiusMeters radio en metros, de 1 a {@value #MAX_RADIUS_METERS}
 * @param nameQuery texto opcional que debe aparecer en el nombre; nulo si no se filtra
 */
public record NearbySearch(GeoPoint center, int radiusMeters, String nameQuery) {

    public static final int DEFAULT_RADIUS_METERS = 1_000;
    public static final int MAX_RADIUS_METERS = 5_000;
    public static final int MAX_NAME_QUERY_LENGTH = 120;

    public NearbySearch {
        Objects.requireNonNull(center, "center");
        if (radiusMeters < 1 || radiusMeters > MAX_RADIUS_METERS) {
            throw new IllegalArgumentException("El radio debe estar entre 1 y " + MAX_RADIUS_METERS + " metros");
        }
        nameQuery = nameQuery == null || nameQuery.isBlank() ? null : nameQuery.strip();
        if (nameQuery != null && nameQuery.length() > MAX_NAME_QUERY_LENGTH) {
            throw new IllegalArgumentException("La búsqueda supera " + MAX_NAME_QUERY_LENGTH + " caracteres");
        }
    }
}
