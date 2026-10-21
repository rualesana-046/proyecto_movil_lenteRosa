package com.lenterosa.api.places.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lenterosa.api.PostgisTestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Pruebas de punta a punta por HTTP real, sin sesión. Los datos se borran después de cada prueba.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgisTestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class PlaceApiIntegrationTest {

    // Centro de las búsquedas: un punto de Pasto. 0.009° ≈ 1000 m.
    private static final String CENTER = "lat=1.2136&lng=-77.2811";

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @AfterEach
    void deleteData() {
        jdbc.update("DELETE FROM reviews");
        jdbc.update("DELETE FROM places");
    }

    @Test
    @DisplayName("Una invitada busca lugares cercanos sin iniciar sesión, del más cercano al más lejano")
    void guestSearchesNearbyPlaces() throws Exception {
        UUID far = insertPlace("Parque lejano", "PARK", 1.2136 + 0.0072, -77.2811);
        UUID near = insertPlace("Parada cercana", "TRANSPORT", 1.2136 + 0.0027, -77.2811);
        insertPlace("Fuera del radio", "OTHER", 1.2136 + 0.0270, -77.2811);

        HttpResponse<String> response = get("/api/v1/places?" + CENTER + "&radiusMeters=1000");
        JsonNode body = json.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.get("totalItems").asLong()).isEqualTo(2);
        JsonNode first = body.get("items").get(0);
        assertThat(first.get("id").asString()).isEqualTo(near.toString());
        assertThat(first.get("name").asString()).isEqualTo("Parada cercana");
        assertThat(first.get("category").asString()).isEqualTo("TRANSPORT");
        assertThat(first.get("location").get("lat").asDouble()).isEqualTo(1.2136 + 0.0027);
        assertThat(first.get("location").get("lng").asDouble()).isEqualTo(-77.2811);
        assertThat(first.get("distanceMeters").asLong()).isBetween(295L, 305L);
        assertThat(body.get("items").get(1).get("id").asString()).isEqualTo(far.toString());
    }

    @Test
    @DisplayName("El detalle trae la cantidad, la fecha más reciente y el promedio de cada dimensión")
    void detailIncludesAveragesPerDimension() throws Exception {
        UUID place = insertPlace("Parque con opiniones", "PARK", 1.2136, -77.2811);
        insertReview(place, 4, 5, 3, null, "DAY", "2026-10-10T15:00:00Z");
        insertReview(place, 2, 2, 2, 2, "NIGHT", "2026-10-12T22:00:00Z");
        insertReview(place, 3, 2, 4, 5, "NIGHT", "2026-10-15T23:30:00Z");

        HttpResponse<String> response = get("/api/v1/places/" + place);
        JsonNode body = json.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.get("reviewCount").asLong()).isEqualTo(3);
        assertThat(body.get("lastReviewAt").asString()).isEqualTo("2026-10-15T23:30:00Z");
        JsonNode averages = body.get("averages");
        assertThat(averages.get("lighting").asDouble()).isEqualTo(3.0);
        assertThat(averages.get("womenPresence").asDouble()).isEqualTo(3.0);
        assertThat(averages.get("commercialVisibility").asDouble()).isEqualTo(3.0);
        // Solo cuentan las dos visitas de noche: (2 + 5) / 2.
        assertThat(averages.get("nightExperience").asDouble()).isEqualTo(3.5);
    }

    @Test
    @DisplayName("Un lugar sin opiniones muestra cantidad 0 y promedios nulos, no ceros")
    void detailWithoutReviewsHasNullAverages() throws Exception {
        UUID place = insertPlace("Lugar sin opiniones", "OTHER", 1.2136, -77.2811);

        JsonNode body = json.readTree(get("/api/v1/places/" + place).body());

        assertThat(body.get("reviewCount").asLong()).isZero();
        assertThat(body.get("lastReviewAt").isNull()).isTrue();
        assertThat(body.get("averages").get("lighting").isNull()).isTrue();
        assertThat(body.get("averages").get("nightExperience").isNull()).isTrue();
    }

    @Test
    @DisplayName("Un lugar inexistente responde 404 con ProblemDetail")
    void unknownPlaceIsNotFound() throws Exception {
        HttpResponse<String> response = get("/api/v1/places/" + UUID.randomUUID());
        JsonNode body = json.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(body.get("title").asString()).isEqualTo("Lugar no encontrado");
        assertThat(body.get("detail").asString()).contains("No encontramos ese lugar");
    }

    @ParameterizedTest(name = "{0} → 400: {1}")
    @CsvSource(delimiter = '|', value = {
        "lat=1.2136&lng=-77.2811&radiusMeters=5001 | El radio de búsqueda debe estar entre 1 y 5000 metros.",
        "lat=1.2136&lng=-77.2811&radiusMeters=0    | El radio de búsqueda debe estar entre 1 y 5000 metros.",
        "lat=1.2136&lng=-77.2811&size=51           | Cada página puede tener entre 1 y 50 resultados.",
        "lat=1.2136&lng=-77.2811&size=0            | Cada página puede tener entre 1 y 50 resultados.",
        "lat=1.2136&lng=-77.2811&page=-1           | El número de página no puede ser negativo.",
        "lat=90.5&lng=-77.2811                     | La latitud debe estar entre -90 y 90.",
        "lat=-91&lng=-77.2811                      | La latitud debe estar entre -90 y 90.",
        "lat=1.2136&lng=-181                       | La longitud debe estar entre -180 y 180.",
        "lat=1.2136&lng=180.5                      | La longitud debe estar entre -180 y 180.",
        "lat=NaN&lng=-77.2811                      | La latitud debe estar entre -90 y 90.",
        "lat=1.2136&lng=Infinity                   | La longitud debe estar entre -180 y 180.",
        "lng=-77.2811                              | Falta el parámetro lat.",
        "lat=abc&lng=-77.2811                      | El valor de lat no tiene el formato esperado."
    })
    @DisplayName("Parámetros fuera de los límites responden 400 con un mensaje claro, sin recortar en silencio")
    void invalidSearchParametersAreRejected(String query, String expectedMessage) throws Exception {
        HttpResponse<String> response = get("/api/v1/places?" + query);
        JsonNode body = json.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(body.get("title").asString()).isEqualTo("Solicitud no válida");
        assertThat(body.get("detail").asString()).contains(expectedMessage);
    }

    @Test
    @DisplayName("Un identificador de lugar mal formado responde 400")
    void malformedPlaceIdIsBadRequest() throws Exception {
        HttpResponse<String> response = get("/api/v1/places/no-es-un-uuid");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(json.readTree(response.body()).get("detail").asString())
                .isEqualTo("El valor de id no tiene el formato esperado.");
    }

    @Test
    @DisplayName("Las coordenadas de la búsqueda no aparecen en los logs")
    void searchCoordinatesAreNotLogged(CapturedOutput output) throws Exception {
        // Valores poco comunes para que no coincidan con nada más del log.
        get("/api/v1/places?lat=1.2137913&lng=-77.2817913");
        get("/api/v1/places?lat=95.4321987&lng=-77.2817913");

        assertThat(output.getAll()).doesNotContain("1.2137913", "77.2817913", "95.4321987");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private UUID insertPlace(String name, String category, double latitude, double longitude) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO places (id, name, category, location)
                VALUES (?, ?, ?, ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography)
                """, id, name, category, longitude, latitude);
        return id;
    }

    private void insertReview(UUID placeId, int lighting, int womenPresence, int commercialVisibility,
            Integer nightExperience, String period, String createdAt) {
        jdbc.update("""
                INSERT INTO reviews (id, place_id, lighting, women_presence, commercial_visibility,
                                     night_experience, visit_period, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::timestamptz)
                """, UUID.randomUUID(), placeId, lighting, womenPresence, commercialVisibility,
                nightExperience, period, createdAt);
    }
}
