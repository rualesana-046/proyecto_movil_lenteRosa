package com.lenterosa.api.users.infrastructure.persistence;

import com.lenterosa.api.users.application.AccountRepository;
import com.lenterosa.api.users.domain.Account;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Adaptador SQL del puerto {@link AccountRepository}. */
@Repository
class JdbcAccountRepository implements AccountRepository {

    private final JdbcClient jdbc;

    JdbcAccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Account> findByUid(String uid) {
        return jdbc.sql("SELECT id, created_at FROM users WHERE id = :uid")
                .param("uid", uid)
                .query((rs, rowNum) -> new Account(rs.getString("id"),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant()))
                .optional();
    }

    @Override
    public Account registerIfAbsent(String uid, Instant registeredAt) {
        Account candidate = new Account(uid, registeredAt);
        jdbc.sql("INSERT INTO users (id, created_at) VALUES (:uid, :createdAt) ON CONFLICT (id) DO NOTHING")
                .param("uid", candidate.uid())
                .param("createdAt", OffsetDateTime.ofInstant(registeredAt, ZoneOffset.UTC))
                .update();
        return findByUid(uid).orElseThrow();
    }

    @Override
    public boolean delete(String uid) {
        return jdbc.sql("DELETE FROM users WHERE id = :uid").param("uid", uid).update() > 0;
    }
}
