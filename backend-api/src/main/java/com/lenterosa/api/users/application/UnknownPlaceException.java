package com.lenterosa.api.users.application;

import java.util.UUID;

/** Se intentó guardar un lugar que no existe. */
public class UnknownPlaceException extends RuntimeException {

    public UnknownPlaceException(UUID placeId) {
        super("No encontramos ese lugar.");
    }
}
