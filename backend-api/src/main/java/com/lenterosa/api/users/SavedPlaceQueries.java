package com.lenterosa.api.users;

import java.util.List;
import java.util.UUID;

/** API pública de lugares guardados para otros módulos (RF-07). */
public interface SavedPlaceQueries {

    /** UIDs de quienes guardaron el lugar. */
    List<String> usersWhoSaved(UUID placeId);
}
