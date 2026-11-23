package com.lenterosa.api.users.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Cuenta registrada. Solo contiene el UID de Firebase y la fecha de alta: la API no guarda correo,
 * nombre ni apodo de las usuarias.
 */
public record Account(String uid, Instant registeredAt) {

    public static final int MAX_UID_LENGTH = 128;

    public Account {
        if (uid == null || uid.isBlank() || uid.length() > MAX_UID_LENGTH) {
            throw new IllegalArgumentException("El UID de la cuenta no es válido");
        }
        Objects.requireNonNull(registeredAt, "registeredAt");
    }
}
