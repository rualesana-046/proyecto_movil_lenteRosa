package com.lenterosa.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// Verifica que las restricciones de V2 y V3 protegen los datos aunque alguien escriba directo en la base.
// Cada prueba corre en una transacción que se revierte al terminar.
@SpringBootTest
@Testcontainers
@Transactional
class MigrationConstraintsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgis = new PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres"));

    @Autowired
    JdbcTemplate jdbc;

    private UUID placeId;

    @BeforeEach
    void insertPlace() {
        placeId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO places (id, name, category, location)
                VALUES (?, 'Lugar de prueba', 'PARK', ST_SetSRID(ST_MakePoint(-77.2811, 1.2136), 4326)::geography)
                """, placeId);
    }

    @Test
    @DisplayName("Una opinión de día sin experiencia nocturna se acepta")
    void acceptsDayReviewWithoutNightExperience() {
        insertReview(4, 4, 3, null, "DAY", null);

        Integer count = jdbc.queryForObject("SELECT count(*) FROM reviews WHERE place_id = ?", Integer.class, placeId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Una opinión de noche con experiencia nocturna se acepta")
    void acceptsNightReviewWithNightExperience() {
        insertReview(2, 3, 2, 2, "NIGHT", "Comentario de prueba");

        Integer count = jdbc.queryForObject("SELECT count(*) FROM reviews WHERE place_id = ?", Integer.class, placeId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Una opinión de noche sin experiencia nocturna se rechaza")
    void rejectsNightReviewWithoutNightExperience() {
        assertThatThrownBy(() -> insertReview(2, 3, 2, null, "NIGHT", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("reviews_night_experience_required");
    }

    // Un solo error por prueba: tras un error, PostgreSQL anula el resto de la transacción.
    @ParameterizedTest(name = "Iluminación con valor {0} se rechaza")
    @ValueSource(ints = {0, 6})
    @DisplayName("Una dimensión obligatoria fuera de 1–5 se rechaza")
    void rejectsRequiredDimensionOutOfRange(int value) {
        assertThatThrownBy(() -> insertReview(value, 3, 3, 3, "NIGHT", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("reviews_lighting_range");
    }

    @ParameterizedTest(name = "Experiencia nocturna con valor {0} se rechaza")
    @ValueSource(ints = {0, 6})
    @DisplayName("La experiencia nocturna fuera de 1–5 se rechaza")
    void rejectsNightExperienceOutOfRange(int value) {
        assertThatThrownBy(() -> insertReview(3, 3, 3, value, "NIGHT", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("reviews_night_experience_range");
    }

    @Test
    @DisplayName("Una dimensión obligatoria nula se rechaza")
    void rejectsMissingRequiredDimension() {
        assertThatThrownBy(() -> insertReview(null, 3, 3, 3, "NIGHT", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Un periodo de visita inválido se rechaza")
    void rejectsInvalidVisitPeriod() {
        assertThatThrownBy(() -> insertReview(3, 3, 3, 3, "EVENING", null))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("reviews_visit_period_valid");
    }

    @Test
    @DisplayName("Un comentario en blanco se rechaza")
    void rejectsBlankComment() {
        assertThatThrownBy(() -> insertReview(3, 3, 3, null, "DAY", "   "))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("reviews_comment_not_blank");
    }

    @Test
    @DisplayName("Una categoría de lugar inválida se rechaza")
    void rejectsInvalidPlaceCategory() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO places (id, name, category, location)
                VALUES (?, 'Otro lugar', 'BEACH', ST_SetSRID(ST_MakePoint(-77.28, 1.21), 4326)::geography)
                """, UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("places_category_valid");
    }

    private void insertReview(Integer lighting, Integer womenPresence, Integer commercialVisibility,
            Integer nightExperience, String visitPeriod, String comment) {
        jdbc.update("""
                INSERT INTO reviews (id, place_id, lighting, women_presence, commercial_visibility,
                                     night_experience, visit_period, comment)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), placeId, lighting, womenPresence, commercialVisibility,
                nightExperience, visitPeriod, comment);
    }
}
