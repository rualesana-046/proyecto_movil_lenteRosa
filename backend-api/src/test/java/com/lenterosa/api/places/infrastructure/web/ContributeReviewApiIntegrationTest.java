package com.lenterosa.api.places.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lenterosa.api.FirebaseTestSecurityConfiguration;
import com.lenterosa.api.FirebaseTestTokens;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Aportar una opinión (RF-04) por HTTP real, con tokens firmados como los de Firebase.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgisTestcontainersConfiguration.class, FirebaseTestSecurityConfiguration.class})
class ContributeReviewApiIntegrationTest {

    private static final String DAY_REVIEW = """
            {"lighting":4,"womenPresence":5,"commercialVisibility":3,"nightExperience":null,
             "visitPeriod":"DAY","comment":"Buena iluminación, queda en la Calle 18 # 20-30"}""";

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    private final String verified = FirebaseTestTokens.valid("uid-ana", true);

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
    @DisplayName("Con sesión y correo verificado la opinión se publica: 201, anónima y con autora guardada")
    void verifiedUserContributes() throws Exception {
        HttpResponse<String> response = post(placeId, verified, DAY_REVIEW);
        JsonNode body = json.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue("Location")).hasValue("/api/v1/places/" + placeId + "/reviews");
        assertThat(response.body()).doesNotContain("uid-ana").doesNotContainIgnoringCase("author");
        assertThat(body.get("lighting").asInt()).isEqualTo(4);
        assertThat(body.get("nightExperience").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT author_id FROM reviews WHERE place_id = ?", String.class, placeId))
                .isEqualTo("uid-ana");
        // El detalle refleja la opinión nueva de inmediato.
        JsonNode detail = json.readTree(get("/api/v1/places/" + placeId).body());
        assertThat(detail.get("reviewCount").asLong()).isEqualTo(1);
        assertThat(detail.get("comfort").get("index").isNull()).isFalse();
    }

    @Test
    @DisplayName("Sin sesión responde 401 y con el correo sin verificar responde 403")
    void requiresSessionAndVerifiedEmail() throws Exception {
        assertThat(post(placeId, null, DAY_REVIEW).statusCode()).isEqualTo(401);

        HttpResponse<String> unverified = post(placeId, FirebaseTestTokens.valid("uid-sin-verificar", false), DAY_REVIEW);

        assertThat(unverified.statusCode()).isEqualTo(403);
        assertThat(json.readTree(unverified.body()).get("detail").asString()).startsWith("Verifica tu correo");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reviews", Integer.class)).isZero();
    }

    @Test
    @DisplayName("Un lugar inexistente responde 404")
    void unknownPlaceIsNotFound() throws Exception {
        assertThat(post(UUID.randomUUID(), verified, DAY_REVIEW).statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("Datos inválidos responden 400 con un mensaje claro: noche sin experiencia nocturna, fuera de rango, JSON roto")
    void invalidDataIsRejected() throws Exception {
        HttpResponse<String> night = post(placeId, verified, """
                {"lighting":4,"womenPresence":5,"commercialVisibility":3,"visitPeriod":"NIGHT"}""");
        HttpResponse<String> outOfRange = post(placeId, verified, """
                {"lighting":6,"womenPresence":5,"commercialVisibility":3,"visitPeriod":"DAY"}""");
        HttpResponse<String> broken = post(placeId, verified, "{no es json");

        assertThat(night.statusCode()).isEqualTo(400);
        assertThat(json.readTree(night.body()).get("detail").asString())
                .isEqualTo("Si fuiste de noche, califica también la experiencia nocturna.");
        assertThat(outOfRange.statusCode()).isEqualTo(400);
        assertThat(json.readTree(outOfRange.body()).get("detail").asString()).isEqualTo("Cada aspecto se califica de 1 a 5.");
        assertThat(broken.statusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("Un comentario con teléfono se rechaza por privacidad; con una dirección se acepta")
    void contactDataIsRejected() throws Exception {
        HttpResponse<String> withPhone = post(placeId, verified, """
                {"lighting":4,"womenPresence":5,"commercialVisibility":3,"visitPeriod":"DAY",
                 "comment":"Escríbeme al +57 300 123 4567"}""");

        assertThat(withPhone.statusCode()).isEqualTo(400);
        JsonNode body = json.readTree(withPhone.body());
        assertThat(body.get("reason").asString()).isEqualTo("CONTACT_DATA");
        assertThat(body.get("detail").asString()).contains("no incluyas datos de contacto ni enlaces");
        assertThat(post(placeId, verified, DAY_REVIEW).statusCode()).isEqualTo(201);
    }

    @Test
    @DisplayName("Una segunda opinión del mismo lugar y periodo responde 429 con Retry-After; otro periodo se permite")
    void samePlaceAndPeriodIsRateLimited() throws Exception {
        assertThat(post(placeId, verified, DAY_REVIEW).statusCode()).isEqualTo(201);

        HttpResponse<String> again = post(placeId, verified, DAY_REVIEW);

        assertThat(again.statusCode()).isEqualTo(429);
        long retryAfter = Long.parseLong(again.headers().firstValue("Retry-After").orElseThrow());
        assertThat(retryAfter).isBetween(7L * 24 * 3_600 - 60, 7L * 24 * 3_600);
        assertThat(json.readTree(again.body()).get("title").asString()).isEqualTo("Límite de opiniones");
        assertThat(post(placeId, verified, """
                {"lighting":2,"womenPresence":2,"commercialVisibility":2,"nightExperience":2,"visitPeriod":"NIGHT"}""")
                .statusCode()).isEqualTo(201);
    }

    private HttpResponse<String> post(UUID place, String bearer, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/v1/places/" + place + "/reviews"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
