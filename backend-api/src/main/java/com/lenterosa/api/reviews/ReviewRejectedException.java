package com.lenterosa.api.reviews;

/** La opinión no se publica porque sus datos no son válidos; el mensaje es apto para mostrar a la usuaria. */
public class ReviewRejectedException extends RuntimeException {

    public enum Reason {
        INVALID,
        CONTACT_DATA
    }

    private final Reason reason;

    public ReviewRejectedException(Reason reason, String userMessage) {
        super(userMessage);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
