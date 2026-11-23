package com.lenterosa.api.reviews;

/** API pública del módulo de opiniones sobre la autoría, para la gestión de cuentas. */
public interface ReviewAuthorship {

    /**
     * Deja anónimas todas las opiniones de una autora ({@code author_id = NULL}) sin borrarlas,
     * como exige la eliminación de una cuenta (Ley 1581 de 2012).
     *
     * @return cantidad de opiniones que quedaron anónimas
     */
    int anonymizeReviewsBy(String authorId);
}
