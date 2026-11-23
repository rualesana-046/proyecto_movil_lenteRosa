package com.lenterosa.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.comfortindex.ComfortIndexQueries;
import com.lenterosa.api.comfortindex.ComfortLabel;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

    private static final UUID PARK = id(1);
    private static final UUID BUS_STOP = id(2);
    private static final UUID SHOP = id(3);
    private static final UUID SCHOOL = id(4);
    private static final UUID HEALTH_CENTER = id(5);
    private static final UUID BAR = id(6);
    private static final UUID STREET = id(7);

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ComfortIndexQueries comfort;

    @Test
    @DisplayName("El seed carga 8 lugares y 24 opiniones, todos marcados como ficticios")
    void seedLoadsFictionalData() {
        List<String> names = jdbc.queryForList("SELECT name FROM places", String.class);

        assertThat(names).hasSize(8).allMatch(name -> name.startsWith("[FICTICIO] "));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reviews", Integer.class)).isEqualTo(24);
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

    @Test
    @DisplayName("Recién cargado, el seed tiene un ejemplo de cada etiqueta del Índice de Confort")
    void seedCoversEveryLabel() {
        Map<UUID, ComfortIndex> indexes = comfort.forPlaces(List.of(PARK, BUS_STOP, SHOP, SCHOOL, HEALTH_CENTER, BAR, STREET));

        assertThat(indexes.get(PARK).label()).isEqualTo(ComfortLabel.MIXED_OPINIONS);
        assertThat(indexes.get(BUS_STOP).label()).isEqualTo(ComfortLabel.COMFORTABLE);
        assertThat(indexes.get(SHOP).label()).isEqualTo(ComfortLabel.VERY_COMFORTABLE);
        assertThat(indexes.get(STREET).label()).isEqualTo(ComfortLabel.UNCOMFORTABLE);
        assertThat(indexes.get(BAR).label()).isEqualTo(ComfortLabel.FEW_OPINIONS);
        assertThat(indexes.get(SCHOOL).index()).isNull();
        // Solo visitas de día: el índice llega a la banda alta, pero la etiqueta se queda en Cómodo.
        assertThat(indexes.get(HEALTH_CENTER).index()).isGreaterThanOrEqualTo(75);
        assertThat(indexes.get(HEALTH_CENTER).nightDataMissing()).isTrue();
        assertThat(indexes.get(HEALTH_CENTER).label()).isEqualTo(ComfortLabel.COMFORTABLE);
    }

    private static UUID id(int place) {
        return UUID.fromString("01929f00-0000-7000-8000-%012d".formatted(place));
    }
}
