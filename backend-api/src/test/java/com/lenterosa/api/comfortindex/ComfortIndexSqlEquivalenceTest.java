package com.lenterosa.api.comfortindex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.lenterosa.api.PostgisTestcontainersConfiguration;
import com.lenterosa.api.comfortindex.domain.ComfortIndexCalculator;
import com.lenterosa.api.comfortindex.domain.RatingSample;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

// Garantiza que las sumas hechas en la base y el cálculo de referencia en memoria dan el mismo índice:
// la fórmula es una sola, aunque una parte se calcule en SQL.
@SpringBootTest
@Import({PostgisTestcontainersConfiguration.class, ComfortIndexSqlEquivalenceTest.FixedClock.class})
@Transactional
class ComfortIndexSqlEquivalenceTest {

    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final double TOLERANCE = 1e-9;

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    ComfortIndexQueries comfort;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("Con opiniones variadas (día, noche, antiguas y futuras) el índice en SQL coincide con la referencia")
    void sqlAggregatesMatchInMemoryReference() {
        Random random = new Random(20261101L);
        for (int place = 0; place < 20; place++) {
            UUID placeId = insertPlace();
            List<RatingSample> samples = new ArrayList<>();
            int reviews = random.nextInt(9);
            for (int i = 0; i < reviews; i++) {
                // Edades entre −2 días (fecha futura) y 400 días, con fracciones de hora.
                Instant createdAt = NOW.minus(Duration.ofMinutes(random.nextInt(400 * 24 * 60 + 2 * 24 * 60) - 2 * 24 * 60));
                boolean night = random.nextBoolean();
                RatingSample sample = new RatingSample(createdAt, 1 + random.nextInt(5), 1 + random.nextInt(5),
                        1 + random.nextInt(5), night ? Integer.valueOf(1 + random.nextInt(5)) : null);
                insertReview(placeId, sample, night);
                samples.add(sample);
            }

            ComfortIndex fromSql = comfort.forPlace(placeId);
            ComfortIndex reference = ComfortIndexCalculator.calculate(samples, NOW);

            assertSame(fromSql, reference);
        }
    }

    private static void assertSame(ComfortIndex actual, ComfortIndex expected) {
        assertThat(actual.index()).isEqualTo(expected.index());
        assertThat(actual.label()).isEqualTo(expected.label());
        assertThat(actual.uncertainty()).isEqualTo(expected.uncertainty());
        assertThat(actual.nightDataMissing()).isEqualTo(expected.nightDataMissing());
        assertThat(actual.effectiveReviews()).isCloseTo(expected.effectiveReviews(), within(TOLERANCE));
        assertThat(actual.dispersion()).isCloseTo(expected.dispersion(), within(1e-6));
        assertClose(actual.dimensions().lighting(), expected.dimensions().lighting());
        assertClose(actual.dimensions().womenPresence(), expected.dimensions().womenPresence());
        assertClose(actual.dimensions().commercialVisibility(), expected.dimensions().commercialVisibility());
        assertClose(actual.dimensions().nightExperience(), expected.dimensions().nightExperience());
    }

    private static void assertClose(Double actual, Double expected) {
        if (expected == null) {
            assertThat(actual).isNull();
        } else {
            assertThat(actual).isCloseTo(expected, within(TOLERANCE));
        }
    }

    private UUID insertPlace() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO places (id, name, category, location)
                VALUES (?, 'Lugar de prueba', 'OTHER', ST_SetSRID(ST_MakePoint(-77.2811, 1.2136), 4326)::geography)
                """, id);
        return id;
    }

    private void insertReview(UUID placeId, RatingSample sample, boolean night) {
        jdbc.update("""
                INSERT INTO reviews (id, place_id, lighting, women_presence, commercial_visibility,
                                     night_experience, visit_period, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::timestamptz)
                """, UUID.randomUUID(), placeId, sample.lighting(), sample.womenPresence(),
                sample.commercialVisibility(), sample.nightExperience(), night ? "NIGHT" : "DAY",
                sample.createdAt().toString());
    }
}
