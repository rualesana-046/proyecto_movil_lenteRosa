package com.lenterosa.api.reviews.application;

import com.lenterosa.api.reviews.DecayedRatingSums;
import com.lenterosa.api.reviews.domain.Review;
import com.lenterosa.api.reviews.domain.VisitPeriod;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Puerto de persistencia de opiniones. Lo implementa la capa de infraestructura. */
public interface ReviewRepository {

    /** Opiniones de un lugar, de la más reciente a la más antigua. */
    Page<Review> findByPlaceId(UUID placeId, Pageable pageable);

    /** Agregados sin redondear de las opiniones de un lugar. */
    RawReviewStats statsForPlace(UUID placeId);

    /** Guarda una opinión nueva con su autora (UID de Firebase). */
    void save(Review review, String authorId);

    /**
     * Bloquea, hasta el final de la transacción, las publicaciones simultáneas de una misma autora,
     * para que dos envíos a la vez no se salten los límites.
     */
    void lockAuthor(String authorId);

    /** Fecha de la opinión más antigua de la autora sobre ese lugar y periodo desde {@code since}, si existe. */
    Optional<Instant> oldestReviewByAuthorSince(String authorId, UUID placeId, VisitPeriod period, Instant since);

    /** Fechas de las opiniones de la autora desde {@code since}, de la más antigua a la más reciente. */
    List<Instant> reviewTimesByAuthorSince(String authorId, Instant since);

    /** Quita la autora de todas sus opiniones ({@code author_id = NULL}); devuelve cuántas cambiaron. */
    int clearAuthor(String authorId);

    /** Sumas ponderadas por antigüedad de las opiniones de cada lugar; solo lugares con opiniones. */
    List<DecayedRatingSums> decayedRatingSums(Collection<UUID> placeIds, Instant now, double halfLifeDays);
}
