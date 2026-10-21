package com.lenterosa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

// Verifica el seed del perfil local sin activarlo: el script corre dentro de la transacción de
// la prueba y se revierte al terminar, así no ensucia la base compartida por las demás pruebas.
@SpringBootTest
@Import(PostgisTestcontainersConfiguration.class)
@Transactional
@Sql("classpath:db/seed/R__seed_fictional_places_pasto.sql")
class LocalSeedTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("El seed carga 8 lugares y 6 opiniones, todos marcados como ficticios")
    void seedLoadsFictionalData() {
        List<String> names = jdbc.queryForList("SELECT name FROM places", String.class);

        assertThat(names).hasSize(8).allMatch(name -> name.startsWith("[FICTICIO] "));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reviews", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reviews WHERE author_id IS NOT NULL", Integer.class))
                .isZero();
    }

    @Test
    @DisplayName("Todos los lugares del seed quedan dentro de Pasto, sin coordenadas invertidas")
    void seedPlacesAreInPasto() {
        Integer outside = jdbc.queryForObject("""
                SELECT count(*) FROM places
                WHERE ST_Y(location::geometry) NOT BETWEEN 1.15 AND 1.25
                   OR ST_X(location::geometry) NOT BETWEEN -77.32 AND -77.24
                """, Integer.class);

        assertThat(outside).isZero();
    }
}
