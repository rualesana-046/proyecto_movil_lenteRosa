package com.lenterosa.api.reviews.domain;

/** Calificación de 1 a 5 de una dimensión de la opinión. */
public record Rating(int value) {

    public static final int MIN = 1;
    public static final int MAX = 5;

    public Rating {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException("La calificación debe estar entre 1 y 5: " + value);
        }
    }
}
