package com.lenterosa.api.users.application;

import java.time.Instant;
import java.util.UUID;

/** Un lugar guardado por la usuaria (RF-05). */
public record SavedPlace(UUID placeId, Instant savedAt) {
}
