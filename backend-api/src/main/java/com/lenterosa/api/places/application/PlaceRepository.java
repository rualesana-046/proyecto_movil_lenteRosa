package com.lenterosa.api.places.application;

import com.lenterosa.api.places.domain.Place;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Puerto de persistencia de lugares. Lo implementa la capa de infraestructura. */
public interface PlaceRepository {

    Optional<Place> findById(UUID id);

    boolean existsById(UUID id);

    /** Lugares dentro del radio, del más cercano al más lejano. */
    Page<NearbyPlace> findNearby(NearbySearch search, Pageable pageable);
}
