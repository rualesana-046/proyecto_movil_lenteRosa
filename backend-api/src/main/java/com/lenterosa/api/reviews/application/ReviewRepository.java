package com.lenterosa.api.reviews.application;

import com.lenterosa.api.reviews.domain.Review;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Puerto de persistencia de opiniones. Lo implementa la capa de infraestructura. */
public interface ReviewRepository {

    /** Opiniones de un lugar, de la más reciente a la más antigua. */
    Page<Review> findByPlaceId(UUID placeId, Pageable pageable);

    /** Agregados sin redondear de las opiniones de un lugar. */
    RawReviewStats statsForPlace(UUID placeId);

    /** Todas las opiniones de los lugares indicados. */
    List<Review> findAllByPlaceIds(Collection<UUID> placeIds);
}
