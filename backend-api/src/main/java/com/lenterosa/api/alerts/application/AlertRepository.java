package com.lenterosa.api.alerts.application;

import com.lenterosa.api.alerts.domain.ComfortSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de avisos. Lo implementa la capa de infraestructura. */
public interface AlertRepository {

    Optional<Alert> findUnread(String uid, UUID placeId);

    void insert(String uid, Alert alert);

    void updateUnread(UUID alertId, ComfortSnapshot current, Instant createdAt);

    void delete(UUID alertId);

    /** Borra los avisos ya leídos anteriores a la fecha dada. */
    void pruneRead(String uid, Instant before);

    /** Del más reciente al más antiguo. */
    List<Alert> listFor(String uid, int limit);

    int countUnread(String uid);

    void markAllRead(String uid, Instant readAt);
}
