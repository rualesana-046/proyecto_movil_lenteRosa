package com.lenterosa.api.comfortindex;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** API pública del Índice de Confort. Se calcula al leer, con todas las opiniones vigentes. */
public interface ComfortIndexQueries {

    ComfortIndex forPlace(UUID placeId);

    /** Índice de varios lugares con una sola consulta de opiniones; incluye todos los ids pedidos. */
    Map<UUID, ComfortIndex> forPlaces(Collection<UUID> placeIds);
}
