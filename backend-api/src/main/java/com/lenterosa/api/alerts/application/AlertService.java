package com.lenterosa.api.alerts.application;

import com.lenterosa.api.alerts.domain.ComfortChange;
import com.lenterosa.api.alerts.domain.ComfortSnapshot;
import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.places.ReviewContributed;
import com.lenterosa.api.shared.UuidV7;
import com.lenterosa.api.users.SavedPlaceQueries;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alertas de cambio en lugares guardados (RF-07). Reacciona a cada opinión publicada, sin procesos
 * programados. Por ahora los avisos quedan en la app; el envío por FCM llega en un paso posterior.
 *
 * <p>Avisos sutiles: un solo aviso sin leer por lugar. Si el lugar vuelve a cambiar antes de que
 * la usuaria lo lea, el aviso se actualiza; si vuelve a como estaba, el aviso desaparece.
 */
public class AlertService {

    /** Avisos que se muestran como máximo. */
    public static final int LIST_LIMIT = 50;

    /** Los avisos leídos se borran después de este tiempo (al llegar un aviso nuevo, sin procesos programados). */
    public static final Duration READ_RETENTION = Duration.ofDays(30);

    private final AlertRepository alerts;
    private final SavedPlaceQueries savedPlaces;
    private final Clock clock;

    public AlertService(AlertRepository alerts, SavedPlaceQueries savedPlaces, Clock clock) {
        this.alerts = alerts;
        this.savedPlaces = savedPlaces;
        this.clock = clock;
    }

    @Transactional
    public void onReviewContributed(ReviewContributed event) {
        ComfortSnapshot before = snapshot(event.before());
        ComfortSnapshot after = snapshot(event.after());
        Instant now = clock.instant();
        for (String uid : savedPlaces.usersWhoSaved(event.placeId())) {
            if (uid.equals(event.authorId())) {
                continue; // No se le avisa a la autora de su propia opinión.
            }
            Optional<Alert> unread = alerts.findUnread(uid, event.placeId());
            // Se compara con cómo estaba el lugar cuando empezó el cambio que aún no se ha leído.
            ComfortSnapshot baseline = unread.map(Alert::previous).orElse(before);
            if (ComfortChange.isRelevant(baseline, after)) {
                if (unread.isPresent()) {
                    alerts.updateUnread(unread.get().id(), after, now);
                } else {
                    alerts.insert(uid, new Alert(UuidV7.next(), event.placeId(), before, after, now, false));
                    alerts.pruneRead(uid, now.minus(READ_RETENTION));
                }
            } else {
                unread.ifPresent(alert -> alerts.delete(alert.id()));
            }
        }
    }

    @Transactional(readOnly = true)
    public AlertInbox inbox(String uid) {
        return new AlertInbox(alerts.countUnread(uid), alerts.listFor(uid, LIST_LIMIT));
    }

    @Transactional
    public void markAllRead(String uid) {
        alerts.markAllRead(uid, clock.instant());
    }

    private static ComfortSnapshot snapshot(ComfortIndex index) {
        return new ComfortSnapshot(index.label(), index.index());
    }

    /** Avisos de la usuaria y cuántos no ha leído. */
    public record AlertInbox(int unreadCount, List<Alert> items) {
    }
}
