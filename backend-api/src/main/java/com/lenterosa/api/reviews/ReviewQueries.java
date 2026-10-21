package com.lenterosa.api.reviews;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** API pública del módulo de opiniones para otros módulos. Solo lectura y siempre anónima. */
public interface ReviewQueries {

    /** Cantidad, fecha de la más reciente y promedio por dimensión de las opiniones de un lugar. */
    ReviewStats statsForPlace(UUID placeId);

    /** Opiniones de un lugar, de la más reciente a la más antigua, sin datos de la autora. */
    Page<PublicReview> listForPlace(UUID placeId, Pageable pageable);
}
