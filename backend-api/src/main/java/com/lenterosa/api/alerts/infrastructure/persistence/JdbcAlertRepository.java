package com.lenterosa.api.alerts.infrastructure.persistence;

import com.lenterosa.api.alerts.application.Alert;
import com.lenterosa.api.alerts.application.AlertRepository;
import com.lenterosa.api.alerts.domain.ComfortSnapshot;
import com.lenterosa.api.comfortindex.ComfortLabel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Adaptador SQL del puerto {@link AlertRepository}. */
@Repository
class JdbcAlertRepository implements AlertRepository {

    private static final String COLUMNS =
            "id, place_id, previous_label, new_label, previous_index, new_index, created_at, read_at";

    private final JdbcClient jdbc;

    JdbcAlertRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Alert> findUnread(String uid, UUID placeId) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM alerts WHERE user_id = :uid AND place_id = :placeId AND read_at IS NULL")
                .param("uid", uid)
                .param("placeId", placeId)
                .query(JdbcAlertRepository::alert)
                .optional();
    }

    @Override
    public void insert(String uid, Alert alert) {
        jdbc.sql("""
                INSERT INTO alerts (id, user_id, place_id, previous_label, new_label, previous_index, new_index, created_at)
                VALUES (:id, :uid, :placeId, :previousLabel, :newLabel, :previousIndex, :newIndex, :createdAt)
                """)
                .param("id", alert.id())
                .param("uid", uid)
                .param("placeId", alert.placeId())
                .param("previousLabel", alert.previous().label().name())
                .param("newLabel", alert.current().label().name())
                .param("previousIndex", alert.previous().index())
                .param("newIndex", alert.current().index())
                .param("createdAt", utc(alert.createdAt()))
                .update();
    }

    @Override
    public void updateUnread(UUID alertId, ComfortSnapshot current, Instant createdAt) {
        jdbc.sql("UPDATE alerts SET new_label = :label, new_index = :index, created_at = :createdAt WHERE id = :id")
                .param("label", current.label().name())
                .param("index", current.index())
                .param("createdAt", utc(createdAt))
                .param("id", alertId)
                .update();
    }

    @Override
    public void delete(UUID alertId) {
        jdbc.sql("DELETE FROM alerts WHERE id = :id").param("id", alertId).update();
    }

    @Override
    public void pruneRead(String uid, Instant before) {
        jdbc.sql("DELETE FROM alerts WHERE user_id = :uid AND read_at IS NOT NULL AND created_at < :before")
                .param("uid", uid)
                .param("before", utc(before))
                .update();
    }

    @Override
    public List<Alert> listFor(String uid, int limit) {
        return jdbc.sql("SELECT " + COLUMNS + " FROM alerts WHERE user_id = :uid ORDER BY created_at DESC LIMIT :limit")
                .param("uid", uid)
                .param("limit", limit)
                .query(JdbcAlertRepository::alert)
                .list();
    }

    @Override
    public int countUnread(String uid) {
        return jdbc.sql("SELECT count(*) FROM alerts WHERE user_id = :uid AND read_at IS NULL")
                .param("uid", uid)
                .query(Integer.class)
                .single();
    }

    @Override
    public void markAllRead(String uid, Instant readAt) {
        jdbc.sql("UPDATE alerts SET read_at = :readAt WHERE user_id = :uid AND read_at IS NULL")
                .param("readAt", utc(readAt))
                .param("uid", uid)
                .update();
    }

    private static Alert alert(ResultSet rs, int rowNum) throws SQLException {
        return new Alert(
                rs.getObject("id", UUID.class),
                rs.getObject("place_id", UUID.class),
                new ComfortSnapshot(ComfortLabel.valueOf(rs.getString("previous_label")), rs.getObject("previous_index", Integer.class)),
                new ComfortSnapshot(ComfortLabel.valueOf(rs.getString("new_label")), rs.getObject("new_index", Integer.class)),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getObject("read_at", OffsetDateTime.class) != null);
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
