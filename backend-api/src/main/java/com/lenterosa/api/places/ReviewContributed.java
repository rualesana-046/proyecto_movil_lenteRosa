package com.lenterosa.api.places;

import com.lenterosa.api.comfortindex.ComfortIndex;
import java.util.Objects;
import java.util.UUID;

/**
 * Se publicó una opinión nueva sobre un lugar (RF-04). Lo usan las alertas de cambio (RF-07).
 *
 * <p>El índice de antes y el de después se calculan en el mismo momento, para que la diferencia
 * se deba solo a la opinión nueva y no al paso del tiempo. {@code authorId} sirve únicamente para
 * no avisarle a la autora de su propia opinión; no sale de la API.
 */
public record ReviewContributed(UUID placeId, String authorId, ComfortIndex before, ComfortIndex after) {

    public ReviewContributed {
        Objects.requireNonNull(placeId, "placeId");
        Objects.requireNonNull(authorId, "authorId");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
    }
}
