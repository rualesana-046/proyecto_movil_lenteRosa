package com.lenterosa.api.places.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PlaceJpaRepository extends JpaRepository<PlaceEntity, UUID> {
}
