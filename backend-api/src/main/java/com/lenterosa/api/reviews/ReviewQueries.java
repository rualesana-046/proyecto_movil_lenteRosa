package com.lenterosa.api.reviews;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** API pública del módulo de opiniones para otros módulos. Solo lectura y siempre anónima. */
public interface ReviewQueries {

    /** Cantidad, fecha de la más reciente y promedio por dimensión de las opiniones de un lugar. */
    ReviewStats statsForPlace(UUID placeId);

    /** Opiniones de un lugar, de la más reciente a la más antigua, sin datos de la autora. */
    Page<PublicReview> listForPlace(UUID placeId, Pageable pageable);

    /** Calificaciones de todas las opiniones de los lugares indicados, en una sola consulta. */
    List<ReviewRatings> ratingsForPlaces(Collection<UUID> placeIds);

    /**
     * Sumas ponderadas por antigüedad de las opiniones de cada lugar, calculadas en la base con una
     * sola consulta. Los lugares sin opiniones no aparecen en la lista.
     */
    List<DecayedRatingSums> decayedRatingSums(Collection<UUID> placeIds, Instant now, double halfLifeDays);
}
