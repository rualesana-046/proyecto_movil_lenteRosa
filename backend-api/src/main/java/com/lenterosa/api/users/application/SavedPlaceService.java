package com.lenterosa.api.users.application;

import com.lenterosa.api.users.SavedPlaceQueries;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lugares guardados de la usuaria autenticada (RF-05). Guardar y quitar son idempotentes.
 *
 * <p>Usa {@link Transactional} porque guardar registra la cuenta si hacía falta (por ejemplo, si
 * la API no respondía cuando inició sesión) y comprueba el límite en la misma operación.
 */
public class SavedPlaceService implements SavedPlaceQueries {

    /** Máximo de lugares guardados por usuaria. */
    public static final int LIMIT = 100;

    private final SavedPlaceRepository saved;
    private final AccountRepository accounts;
    private final Clock clock;

    public SavedPlaceService(SavedPlaceRepository saved, AccountRepository accounts, Clock clock) {
        this.saved = saved;
        this.accounts = accounts;
        this.clock = clock;
    }

    @Transactional
    public void save(String uid, UUID placeId) {
        accounts.registerIfAbsent(uid, clock.instant());
        if (saved.isSaved(uid, placeId)) {
            return;
        }
        if (saved.count(uid) >= LIMIT) {
            throw new SavedPlaceLimitException(LIMIT);
        }
        if (!saved.saveIfAbsent(uid, placeId, clock.instant())) {
            throw new UnknownPlaceException(placeId);
        }
    }

    @Transactional
    public void remove(String uid, UUID placeId) {
        saved.remove(uid, placeId);
    }

    @Transactional(readOnly = true)
    public List<SavedPlace> list(String uid) {
        return saved.listFor(uid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> usersWhoSaved(UUID placeId) {
        return saved.usersWhoSaved(placeId);
    }
}
