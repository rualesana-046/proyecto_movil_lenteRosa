package com.lenterosa.api.alerts.application;

import static com.lenterosa.api.comfortindex.ComfortLabel.COMFORTABLE;
import static com.lenterosa.api.comfortindex.ComfortLabel.FEW_OPINIONS;
import static com.lenterosa.api.comfortindex.ComfortLabel.MIXED_OPINIONS;
import static com.lenterosa.api.comfortindex.ComfortLabel.VERY_COMFORTABLE;
import static org.assertj.core.api.Assertions.assertThat;

import com.lenterosa.api.alerts.domain.ComfortSnapshot;
import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.comfortindex.ComfortLabel;
import com.lenterosa.api.comfortindex.DimensionComfort;
import com.lenterosa.api.comfortindex.Uncertainty;
import com.lenterosa.api.places.ReviewContributed;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AlertServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final UUID PLACE = UUID.randomUUID();

    private final InMemoryAlerts repository = new InMemoryAlerts();
    private final Map<UUID, List<String>> savers = new HashMap<>();
    private final AlertService service =
            new AlertService(repository, placeId -> savers.getOrDefault(placeId, List.of()), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("Un cambio relevante avisa a quien guardó el lugar, pero no a la autora de la opinión")
    void relevantChangeNotifiesSaversExceptAuthor() {
        savers.put(PLACE, List.of("uid-bea", "uid-ana"));

        service.onReviewContributed(event("uid-ana", COMFORTABLE, 66, MIXED_OPINIONS, 55));

        assertThat(repository.unread("uid-bea")).singleElement().satisfies(alert -> {
            assertThat(alert.previous()).isEqualTo(new ComfortSnapshot(COMFORTABLE, 66));
            assertThat(alert.current()).isEqualTo(new ComfortSnapshot(MIXED_OPINIONS, 55));
        });
        assertThat(repository.unread("uid-ana")).isEmpty();
    }

    @Test
    @DisplayName("Un cambio pequeño no avisa")
    void smallChangeDoesNotNotify() {
        savers.put(PLACE, List.of("uid-bea"));

        service.onReviewContributed(event("uid-ana", COMFORTABLE, 66, COMFORTABLE, 64));

        assertThat(repository.unread("uid-bea")).isEmpty();
    }

    @Test
    @DisplayName("Si el lugar vuelve a cambiar antes de leer el aviso, se actualiza el mismo aviso")
    void secondChangeUpdatesTheUnreadAlert() {
        savers.put(PLACE, List.of("uid-bea"));
        service.onReviewContributed(event("uid-ana", VERY_COMFORTABLE, 80, COMFORTABLE, 68));

        service.onReviewContributed(event("uid-cami", COMFORTABLE, 68, MIXED_OPINIONS, 52));

        assertThat(repository.unread("uid-bea")).singleElement().satisfies(alert -> {
            assertThat(alert.previous()).isEqualTo(new ComfortSnapshot(VERY_COMFORTABLE, 80));
            assertThat(alert.current()).isEqualTo(new ComfortSnapshot(MIXED_OPINIONS, 52));
        });
    }

    @Test
    @DisplayName("Al pasar de Pocas opiniones a tener etiqueta no hay aviso; si un aviso pendiente queda en Pocas opiniones, se retira")
    void fewOpinionsDoNotNotify() {
        savers.put(PLACE, List.of("uid-bea"));

        service.onReviewContributed(event("uid-ana", FEW_OPINIONS, null, COMFORTABLE, 68));
        assertThat(repository.unread("uid-bea")).isEmpty();

        service.onReviewContributed(event("uid-cami", COMFORTABLE, 68, MIXED_OPINIONS, 52));
        assertThat(repository.unread("uid-bea")).hasSize(1);
        service.onReviewContributed(event("uid-dani", MIXED_OPINIONS, 52, FEW_OPINIONS, 50));
        assertThat(repository.unread("uid-bea")).isEmpty();
    }

    @Test
    @DisplayName("Si el lugar vuelve a como estaba antes de leer el aviso, el aviso desaparece")
    void revertedChangeRemovesTheAlert() {
        savers.put(PLACE, List.of("uid-bea"));
        service.onReviewContributed(event("uid-ana", COMFORTABLE, 66, MIXED_OPINIONS, 55));

        service.onReviewContributed(event("uid-cami", MIXED_OPINIONS, 55, COMFORTABLE, 64));

        assertThat(repository.unread("uid-bea")).isEmpty();
    }

    @Test
    @DisplayName("Marcar como leídos deja la bandeja sin avisos pendientes")
    void markAllRead() {
        savers.put(PLACE, List.of("uid-bea"));
        service.onReviewContributed(event("uid-ana", COMFORTABLE, 66, MIXED_OPINIONS, 55));

        service.markAllRead("uid-bea");

        assertThat(service.inbox("uid-bea").unreadCount()).isZero();
        assertThat(service.inbox("uid-bea").items()).singleElement().extracting(Alert::read).isEqualTo(true);
    }

    private static ReviewContributed event(String author, ComfortLabel beforeLabel, Integer beforeIndex, ComfortLabel afterLabel,
            Integer afterIndex) {
        return new ReviewContributed(PLACE, author, index(beforeLabel, beforeIndex), index(afterLabel, afterIndex));
    }

    private static ComfortIndex index(ComfortLabel label, Integer value) {
        return new ComfortIndex(value, label, Uncertainty.MEDIUM, 4, 0.5, false, DimensionComfort.none(), 1);
    }

    private static final class InMemoryAlerts implements AlertRepository {

        private record Row(String uid, Alert alert) {
        }

        private final List<Row> rows = new ArrayList<>();

        List<Alert> unread(String uid) {
            return rows.stream().filter(r -> r.uid().equals(uid) && !r.alert().read()).map(Row::alert).toList();
        }

        @Override
        public Optional<Alert> findUnread(String uid, UUID placeId) {
            return unread(uid).stream().filter(a -> a.placeId().equals(placeId)).findFirst();
        }

        @Override
        public void insert(String uid, Alert alert) {
            rows.add(new Row(uid, alert));
        }

        @Override
        public void updateUnread(UUID alertId, ComfortSnapshot current, Instant createdAt) {
            rows.replaceAll(r -> r.alert().id().equals(alertId)
                    ? new Row(r.uid(), new Alert(alertId, r.alert().placeId(), r.alert().previous(), current, createdAt, false))
                    : r);
        }

        @Override
        public void delete(UUID alertId) {
            rows.removeIf(r -> r.alert().id().equals(alertId));
        }

        @Override
        public void pruneRead(String uid, Instant before) {
            rows.removeIf(r -> r.uid().equals(uid) && r.alert().read() && r.alert().createdAt().isBefore(before));
        }

        @Override
        public List<Alert> listFor(String uid, int limit) {
            return rows.stream().filter(r -> r.uid().equals(uid)).map(Row::alert)
                    .sorted(Comparator.comparing(Alert::createdAt).reversed()).limit(limit).toList();
        }

        @Override
        public int countUnread(String uid) {
            return unread(uid).size();
        }

        @Override
        public void markAllRead(String uid, Instant readAt) {
            rows.replaceAll(r -> r.uid().equals(uid)
                    ? new Row(uid, new Alert(r.alert().id(), r.alert().placeId(), r.alert().previous(), r.alert().current(),
                            r.alert().createdAt(), true))
                    : r);
        }
    }

}
