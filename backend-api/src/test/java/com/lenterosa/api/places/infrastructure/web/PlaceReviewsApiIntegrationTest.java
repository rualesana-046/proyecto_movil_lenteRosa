package com.lenterosa.api.places.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lenterosa.api.PostgisTestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Pruebas de punta a punta por HTTP real, sin sesión. Los datos se borran después de cada prueba.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgisTestcontainersConfiguration.class)
class PlaceReviewsApiIntegrationTest {

    // UID ficticio de autora: nunca debe aparecer en una respuesta.
    private static final String AUTHOR_ID = "uid-de-prueba-que-no-debe-salir";

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    private UUID placeId;

    @BeforeEach
    void insertPlace() {
        placeId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO places (id, name, category, location)
                VALUES (?, 'Parque de prueba', 'PARK', ST_SetSRID(ST_MakePoint(-77.2811, 1.2136), 4326)::geography)
                """, placeId);
    }

    @AfterEach
    void deleteData() {
        jdbc.update("DELETE FROM reviews");
        jdbc.update("DELETE FROM places");
    }

    @Test
    @DisplayName("Las opiniones salen anónimas: sin autora ni su identificador")
    void reviewsAreAnonymous() throws Exception {
        insertReview(4, 4, 3, null, "DAY", "Comentario de prueba", "2026-10-10T15:00:00Z");

        HttpResponse<String> response = get("/api/v1/places/" + placeId + "/reviews");
        JsonNode review = json.readTree(response.body()).get("items").get(0);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).doesNotContain(AUTHOR_ID).doesNotContainIgnoringCase("author");
        assertThat(review.propertyNames()).containsExactlyInAnyOrder(
                "id", "lighting", "womenPresence", "commercialVisibility", "nightExperience",
                "visitPeriod", "comment", "createdAt");
    }

    @Test
    @DisplayName("Cada opinión trae sus dimensiones; de día la experiencia nocturna puede ser nula")
    void reviewFieldsAreMapped() throws Exception {
        insertReview(4, 5, 3, null, "DAY", "Comentario de prueba", "2026-10-10T15:00:00Z");

        JsonNode review = json.readTree(get("/api/v1/places/" + placeId + "/reviews").body()).get("items").get(0);

        assertThat(review.get("lighting").asInt()).isEqualTo(4);
        assertThat(review.get("womenPresence").asInt()).isEqualTo(5);
        assertThat(review.get("commercialVisibility").asInt()).isEqualTo(3);
        assertThat(review.get("nightExperience").isNull()).isTrue();
        assertThat(review.get("visitPeriod").asString()).isEqualTo("DAY");
        assertThat(review.get("comment").asString()).isEqualTo("Comentario de prueba");
        assertThat(review.get("createdAt").asString()).isEqualTo("2026-10-10T15:00:00Z");
    }

    @Test
    @DisplayName("Van de la más reciente a la más antigua y se paginan")
    void reviewsAreNewestFirstAndPaged() throws Exception {
        insertReview(1, 1, 1, 1, "NIGHT", null, "2026-10-01T22:00:00Z");
        insertReview(2, 2, 2, 2, "NIGHT", null, "2026-10-02T22:00:00Z");
        insertReview(3, 3, 3, 3, "NIGHT", null, "2026-10-03T22:00:00Z");

        JsonNode first = json.readTree(get("/api/v1/places/" + placeId + "/reviews?size=2").body());
        JsonNode second = json.readTree(get("/api/v1/places/" + placeId + "/reviews?size=2&page=1").body());

        assertThat(first.get("totalItems").asLong()).isEqualTo(3);
        assertThat(first.get("totalPages").asInt()).isEqualTo(2);
        assertThat(first.get("items").get(0).get("lighting").asInt()).isEqualTo(3);
        assertThat(first.get("items").get(1).get("lighting").asInt()).isEqualTo(2);
        assertThat(second.get("items").get(0).get("lighting").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("Las opiniones de un lugar inexistente responden 404")
    void unknownPlaceIsNotFound() throws Exception {
        HttpResponse<String> response = get("/api/v1/places/" + UUID.randomUUID() + "/reviews");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(json.readTree(response.body()).get("title").asString()).isEqualTo("Lugar no encontrado");
    }

    @ParameterizedTest(name = "{0} → 400: {1}")
    @CsvSource(delimiter = '|', value = {
        "size=51 | Cada página puede tener entre 1 y 50 resultados.",
        "size=0  | Cada página puede tener entre 1 y 50 resultados.",
        "page=-1 | El número de página no puede ser negativo."
    })
    @DisplayName("Una paginación fuera de los límites responde 400 con un mensaje claro")
    void invalidPagingIsRejected(String query, String expectedMessage) throws Exception {
        HttpResponse<String> response = get("/api/v1/places/" + placeId + "/reviews?" + query);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json.readTree(response.body()).get("detail").asString()).contains(expectedMessage);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void insertReview(int lighting, int womenPresence, int commercialVisibility, Integer nightExperience,
            String period, String comment, String createdAt) {
        jdbc.update("""
                INSERT INTO reviews (id, place_id, author_id, lighting, women_presence, commercial_visibility,
                                     night_experience, visit_period, comment, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::timestamptz)
                """, UUID.randomUUID(), placeId, AUTHOR_ID, lighting, womenPresence, commercialVisibility,
                nightExperience, period, comment, createdAt);
    }
}
