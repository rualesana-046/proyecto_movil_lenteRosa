package com.lenterosa.api.places.application;

import java.util.UUID;

/** El lugar pedido no existe. La capa web la traduce a 404. */
public class PlaceNotFoundException extends RuntimeException {

    private final UUID placeId;

    public PlaceNotFoundException(UUID placeId) {
        super("No existe el lugar " + placeId);
        this.placeId = placeId;
    }

    public UUID placeId() {
        return placeId;
    }
}
