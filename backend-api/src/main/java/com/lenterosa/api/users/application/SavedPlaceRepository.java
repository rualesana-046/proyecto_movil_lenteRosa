package com.lenterosa.api.users.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Puerto de persistencia de lugares guardados. Lo implementa la capa de infraestructura. */
public interface SavedPlaceRepository {

    /** Guarda el lugar si no estaba guardado. Devuelve falso si el lugar no existe. */
    boolean saveIfAbsent(String uid, UUID placeId, Instant savedAt);

    void remove(String uid, UUID placeId);

    boolean isSaved(String uid, UUID placeId);

    int count(String uid);

    /** Del más reciente al más antiguo. */
    List<SavedPlace> listFor(String uid);

    List<String> usersWhoSaved(UUID placeId);
}
