package com.lenterosa.api.reviews;

import java.time.Duration;

/** Se superó el límite contra abuso; {@code retryAfter} indica cuánto falta para poder opinar de nuevo. */
public class ReviewRateLimitedException extends RuntimeException {

    private final Duration retryAfter;

    public ReviewRateLimitedException(String userMessage, Duration retryAfter) {
        super(userMessage);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
