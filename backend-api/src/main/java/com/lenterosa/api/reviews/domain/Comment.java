package com.lenterosa.api.reviews.domain;

/** Comentario opcional de una opinión, sin espacios sobrantes y de hasta 500 caracteres. */
public record Comment(String text) {

    public static final int MAX_LENGTH = 500;

    public Comment {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Un comentario no puede estar en blanco");
        }
        text = text.strip();
        if (text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("El comentario supera " + MAX_LENGTH + " caracteres");
        }
    }

    /** Devuelve {@code null} cuando no hay comentario (nulo o en blanco). */
    public static Comment ofNullable(String text) {
        return text == null || text.isBlank() ? null : new Comment(text);
    }
}
