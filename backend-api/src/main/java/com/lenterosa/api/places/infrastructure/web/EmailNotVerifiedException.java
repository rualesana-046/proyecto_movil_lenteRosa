package com.lenterosa.api.places.infrastructure.web;

/** La usuaria inició sesión pero no ha verificado su correo: no puede opinar (ADR 0006). */
class EmailNotVerifiedException extends RuntimeException {

    EmailNotVerifiedException() {
        super("Correo sin verificar");
    }
}
