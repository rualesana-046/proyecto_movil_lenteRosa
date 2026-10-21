package com.lenterosa.api.places.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Lugar que la comunidad valora. Las mismas reglas están como restricciones en la tabla {@code places}. */
public record Place(UUID id, String name, PlaceCategory category, String address, GeoPoint location, Instant createdAt) {

    public static final int MAX_NAME_LENGTH = 120;
    public static final int MAX_ADDRESS_LENGTH = 200;

    public Place {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(createdAt, "createdAt");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del lugar es obligatorio");
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("El nombre supera " + MAX_NAME_LENGTH + " caracteres");
        }
        // Una dirección en blanco se guarda como ausente.
        address = address == null || address.isBlank() ? null : address.strip();
        if (address != null && address.length() > MAX_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("La dirección supera " + MAX_ADDRESS_LENGTH + " caracteres");
        }
    }
}
