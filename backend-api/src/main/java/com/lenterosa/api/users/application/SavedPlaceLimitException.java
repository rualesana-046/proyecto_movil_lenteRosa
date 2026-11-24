package com.lenterosa.api.users.application;

/** La usuaria llegó al máximo de lugares guardados. */
public class SavedPlaceLimitException extends RuntimeException {

    public SavedPlaceLimitException(int limit) {
        super("Puedes guardar hasta " + limit + " lugares. Quita alguno para guardar otro.");
    }
}
