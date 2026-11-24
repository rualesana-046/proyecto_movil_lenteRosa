package com.lenterosa.api.users.infrastructure.persistence;

import com.lenterosa.api.users.application.SavedPlace;
import com.lenterosa.api.users.application.SavedPlaceRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Adaptador SQL del puerto {@link SavedPlaceRepository}. */
@Repository
class JdbcSavedPlaceRepository implements SavedPlaceRepository {

    private final JdbcClient jdbc;

    JdbcSavedPlaceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // La llave foránea a places dice si el lugar existe: así no se lee la tabla de otro módulo. La
    // cuenta ya está registrada antes de llamar, así que la única llave que puede fallar es la del lugar.
    @Override
    public boolean saveIfAbsent(String uid, UUID placeId, Instant savedAt) {
        try {
            jdbc.sql("""
                    INSERT INTO saved_places (user_id, place_id, created_at) VALUES (:uid, :placeId, :savedAt)
                    ON CONFLICT (user_id, place_id) DO NOTHING
                    """)
                    .param("uid", uid)
                    .param("placeId", placeId)
                    .param("savedAt", OffsetDateTime.ofInstant(savedAt, ZoneOffset.UTC))
                    .update();
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    @Override
    public void remove(String uid, UUID placeId) {
        jdbc.sql("DELETE FROM saved_places WHERE user_id = :uid AND place_id = :placeId")
                .param("uid", uid)
                .param("placeId", placeId)
                .update();
    }

    @Override
    public boolean isSaved(String uid, UUID placeId) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM saved_places WHERE user_id = :uid AND place_id = :placeId)")
                .param("uid", uid)
                .param("placeId", placeId)
                .query(Boolean.class)
                .single();
    }

    @Override
    public int count(String uid) {
        return jdbc.sql("SELECT count(*) FROM saved_places WHERE user_id = :uid")
                .param("uid", uid)
                .query(Integer.class)
                .single();
    }

    @Override
    public List<SavedPlace> listFor(String uid) {
        return jdbc.sql("SELECT place_id, created_at FROM saved_places WHERE user_id = :uid ORDER BY created_at DESC")
                .param("uid", uid)
                .query((rs, rowNum) -> new SavedPlace(rs.getObject("place_id", UUID.class),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant()))
                .list();
    }

    @Override
    public List<String> usersWhoSaved(UUID placeId) {
        return jdbc.sql("SELECT user_id FROM saved_places WHERE place_id = :placeId")
                .param("placeId", placeId)
                .query(String.class)
                .list();
    }
}
