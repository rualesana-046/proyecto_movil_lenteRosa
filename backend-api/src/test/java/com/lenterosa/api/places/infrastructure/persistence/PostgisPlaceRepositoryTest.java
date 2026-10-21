package com.lenterosa.api.places.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.lenterosa.api.PostgisTestcontainersConfiguration;
import com.lenterosa.api.places.application.NearbyPlace;
import com.lenterosa.api.places.application.NearbySearch;
import com.lenterosa.api.places.application.PlaceRepository;
import com.lenterosa.api.places.domain.GeoPoint;
import com.lenterosa.api.places.domain.Place;
import com.lenterosa.api.places.domain.PlaceCategory;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

// Cada prueba corre en una transacción que se revierte al terminar.
@SpringBootTest
@Import(PostgisTestcontainersConfiguration.class)
@Transactional
class PostgisPlaceRepositoryTest {

    private static final GeoPoint PASTO = new GeoPoint(1.2136, -77.2811);
    // Cerca del ecuador, 0.009° de latitud ≈ 1000 m y 0.009° de longitud ≈ 1000 m.
    private static final double DEGREES_PER_KM = 0.009;
    private static final Instant CREATED_AT = Instant.parse("2026-10-01T12:00:00Z");

    @Autowired
    PlaceRepository repository;

    @Autowired
    PlaceJpaRepository jpa;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("Pasto se guarda como (longitud, latitud) en PostGIS, sin invertirse")
    void storesLongitudeAsXAndLatitudeAsY() {
        Place place = save("Lugar en Pasto", PASTO);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT ST_X(location::geometry) AS x, ST_Y(location::geometry) AS y FROM places WHERE id = ?",
                place.id());

        assertThat((Double) row.get("x")).as("x = longitud").isCloseTo(-77.2811, within(1e-9));
        assertThat((Double) row.get("y")).as("y = latitud").isCloseTo(1.2136, within(1e-9));
    }

    @Test
    @DisplayName("Un lugar se lee con la misma latitud y longitud con que se guardó")
    void readsBackSameCoordinates() {
        Place saved = save("Lugar en Pasto", PASTO);

        Place read = repository.findById(saved.id()).orElseThrow();

        assertThat(read.location().latitude()).isCloseTo(1.2136, within(1e-9));
        assertThat(read.location().longitude()).isCloseTo(-77.2811, within(1e-9));
        assertThat(read).isEqualTo(saved);
    }

    @Test
    @DisplayName("La distancia calculada es en metros y coincide con la geografía real")
    void distanceIsInMetersOnTheEarth() {
        save("A un kilómetro al norte", north(1.0));

        NearbyPlace found = search(PASTO, 5_000, null, 0, 10).getContent().getFirst();

        // 0.009° de latitud son ~1000 m. Si el punto estuviera invertido, la distancia sería enorme.
        assertThat(found.distanceMeters()).isCloseTo(1_000, within(10.0));
    }

    @Test
    @DisplayName("Solo aparecen los lugares dentro del radio")
    void respectsRadius() {
        Place near = save("A 500 m", north(0.5));
        save("A 2 km", east(2.0));

        Page<NearbyPlace> page = search(PASTO, 1_000, null, 0, 10);

        assertThat(page.getContent()).extracting(n -> n.place().id()).containsExactly(near.id());
    }

    @Test
    @DisplayName("Los resultados van del más cercano al más lejano y se paginan")
    void ordersByDistanceAndPaginates() {
        Place far = save("A 900 m", north(0.9));
        Place nearest = save("A 300 m", east(0.3));
        Place middle = save("A 600 m", north(0.6));

        Page<NearbyPlace> first = search(PASTO, 1_000, null, 0, 2);
        Page<NearbyPlace> second = search(PASTO, 1_000, null, 1, 2);

        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getContent()).extracting(n -> n.place().id()).containsExactly(nearest.id(), middle.id());
        assertThat(second.getContent()).extracting(n -> n.place().id()).containsExactly(far.id());
    }

    @Test
    @DisplayName("La búsqueda por nombre no distingue mayúsculas")
    void nameFilterIsCaseInsensitive() {
        Place park = save("PARQUE de Prueba", north(0.2));
        save("Tienda de prueba", north(0.3));

        Page<NearbyPlace> page = search(PASTO, 1_000, "parque", 0, 10);

        assertThat(page.getContent()).extracting(n -> n.place().id()).containsExactly(park.id());
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("% y _ en la búsqueda se buscan literal, no como comodines")
    void nameFilterTreatsWildcardsLiterally() {
        Place percent = save("Tienda 100% local", north(0.2));
        save("Tienda normal", north(0.3));
        Place underscore = save("Parque_Norte", north(0.4));
        // Con _ como comodín, "e_N" también encontraría este lugar.
        save("ParqueXNorte", north(0.5));

        assertThat(search(PASTO, 1_000, "%", 0, 10).getContent())
                .extracting(n -> n.place().id()).containsExactly(percent.id());
        assertThat(search(PASTO, 1_000, "e_N", 0, 10).getContent())
                .extracting(n -> n.place().id()).containsExactly(underscore.id());
    }

    @Test
    @DisplayName("Un id inexistente no encuentra nada")
    void unknownIdIsEmpty() {
        UUID unknown = UUID.randomUUID();

        assertThat(repository.findById(unknown)).isEmpty();
        assertThat(repository.existsById(unknown)).isFalse();
    }

    private Page<NearbyPlace> search(GeoPoint center, int radiusMeters, String name, int page, int size) {
        return repository.findNearby(new NearbySearch(center, radiusMeters, name), PageRequest.of(page, size));
    }

    private Place save(String name, GeoPoint location) {
        Place place = new Place(UUID.randomUUID(), name, PlaceCategory.OTHER, null, location, CREATED_AT);
        jpa.saveAndFlush(PlaceEntity.fromDomain(place));
        return place;
    }

    private static GeoPoint north(double km) {
        return new GeoPoint(PASTO.latitude() + km * DEGREES_PER_KM, PASTO.longitude());
    }

    private static GeoPoint east(double km) {
        return new GeoPoint(PASTO.latitude(), PASTO.longitude() + km * DEGREES_PER_KM);
    }
}
