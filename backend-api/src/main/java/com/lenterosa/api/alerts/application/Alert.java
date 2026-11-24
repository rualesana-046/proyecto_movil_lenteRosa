package com.lenterosa.api.alerts.application;

import com.lenterosa.api.alerts.domain.ComfortSnapshot;
import java.time.Instant;
import java.util.UUID;

/**
 * Aviso de que cambió la percepción de un lugar guardado. No dice quién opinó.
 *
 * @param previous cómo estaba el lugar antes del primer cambio sin leer
 * @param current cómo está ahora
 */
public record Alert(UUID id, UUID placeId, ComfortSnapshot previous, ComfortSnapshot current, Instant createdAt, boolean read) {
}
