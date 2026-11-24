package com.lenterosa.api.reviews;

/** API pública del módulo de opiniones para publicar una opinión (RF-04). */
public interface ReviewSubmissions {

    /**
     * Publica la opinión y la devuelve tal como se muestra a todas: anónima.
     *
     * @throws ReviewRejectedException si los datos no son válidos o el comentario trae datos de contacto
     * @throws ReviewRateLimitedException si se superó el límite contra abuso
     */
    PublicReview submit(ReviewSubmission submission);
}
