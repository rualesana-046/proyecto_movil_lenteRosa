package com.lenterosa.api.alerts.infrastructure.web;

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

// Lugares guardados (RF-05) y avisos de cambio (RF-07) por HTTP real, con tokens firmados como los de Firebase.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgisTestcontainersConfiguration.class, FirebaseTestSecurityConfiguration.class})
class SavedPlacesAndAlertsApiIntegrationTest {

    private static final String GOOD_DAY = """
            {"lighting":5,"womenPresence":5,"commercialVisibility":5,"visitPeriod":"DAY"}""";
    private static final String BAD_NIGHT = """
            {"lighting":1,"womenPresence":1,"commercialVisibility":1,"nightExperience":1,"visitPeriod":"NIGHT"}""";

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();
    private final String bea = FirebaseTestTokens.valid("uid-bea", true);
    private final String ana = FirebaseTestTokens.valid("uid-ana", true);
    private final String cami = FirebaseTestTokens.valid("uid-cami", true);
    private final String dani = FirebaseTestTokens.valid("uid-dani", true);

    private UUID placeId;

    @BeforeEach
    void insertPlace() {
        placeId = insertPlace("Parque de prueba");
    }

    @AfterEach
    void deleteData() {
        jdbc.update("DELETE FROM alerts");
        jdbc.update("DELETE FROM saved_places");
        jdbc.update("DELETE FROM reviews");
        jdbc.update("DELETE FROM users");
        jdbc.update("DELETE FROM places");
    }

    @Test
    @DisplayName("Guardar y quitar un lugar es idempotente y la lista refleja el cambio")
    void saveAndRemove() throws Exception {
        assertThat(send("PUT", "/api/v1/me/saved-places/" + placeId, bea).statusCode()).isEqualTo(204);
        assertThat(send("PUT", "/api/v1/me/saved-places/" + placeId, bea).statusCode()).isEqualTo(204);

        JsonNode saved = json.readTree(send("GET", "/api/v1/me/saved-places", bea).body());
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).get("placeId").asString()).isEqualTo(placeId.toString());

        assertThat(send("DELETE", "/api/v1/me/saved-places/" + placeId, bea).statusCode()).isEqualTo(204);
        assertThat(send("DELETE", "/api/v1/me/saved-places/" + placeId, bea).statusCode()).isEqualTo(204);
        assertThat(json.readTree(send("GET", "/api/v1/me/saved-places", bea).body())).isEmpty();
    }

    @Test
    @DisplayName("Sin sesión responde 401 y un lugar inexistente responde 404")
    void requiresSessionAndExistingPlace() throws Exception {
        assertThat(send("PUT", "/api/v1/me/saved-places/" + placeId, null).statusCode()).isEqualTo(401);
        assertThat(send("GET", "/api/v1/me/alerts", null).statusCode()).isEqualTo(401);

        HttpResponse<String> unknown = send("PUT", "/api/v1/me/saved-places/" + UUID.randomUUID(), bea);

        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(json.readTree(unknown.body()).get("detail").asString()).isEqualTo("No encontramos ese lugar.");
    }

    @Test
    @DisplayName("Se pueden guardar hasta 100 lugares; el siguiente responde 409 con un mensaje claro")
    void limitOfOneHundred() throws Exception {
        jdbc.update("INSERT INTO users (id) VALUES ('uid-bea')");
        jdbc.update("""
                INSERT INTO places (id, name, category, location)
                SELECT gen_random_uuid(), 'Lugar ' || i, 'PARK', ST_SetSRID(ST_MakePoint(-77.28, 1.21), 4326)::geography
                FROM generate_series(1, 100) AS i
                """);
        jdbc.update("INSERT INTO saved_places (user_id, place_id) SELECT 'uid-bea', id FROM places WHERE name LIKE 'Lugar %'");

        HttpResponse<String> response = send("PUT", "/api/v1/me/saved-places/" + placeId, bea);

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(json.readTree(response.body()).get("detail").asString()).startsWith("Puedes guardar hasta 100 lugares");
    }

    @Test
    @DisplayName("Mientras el lugar tiene pocas opiniones no hay avisos, aunque luego reciba etiqueta")
    void noAlertsWhileFewOpinions() throws Exception {
        send("PUT", "/api/v1/me/saved-places/" + placeId, bea);

        assertThat(post(placeId, ana, GOOD_DAY).statusCode()).isEqualTo(201);
        assertThat(post(placeId, dani, GOOD_DAY).statusCode()).isEqualTo(201);
        // La tercera opinión le da etiqueta al lugar, pero antes no había evidencia suficiente para comparar.
        assertThat(post(placeId, cami, GOOD_DAY).statusCode()).isEqualTo(201);
        assertThat(json.readTree(send("GET", "/api/v1/places/" + placeId, null).body()).get("comfort").get("label").asString())
                .isNotEqualTo("FEW_OPINIONS");

        assertThat(json.readTree(send("GET", "/api/v1/me/alerts", bea).body()).get("unreadCount").asInt()).isZero();
    }

    @Test
    @DisplayName("Con evidencia suficiente, un cambio de etiqueta avisa a quien guardó el lugar, sin decir quién opinó")
    void relevantChangeCreatesAlert() throws Exception {
        insertGoodHistory(placeId, 4);
        String before = json.readTree(send("GET", "/api/v1/places/" + placeId, null).body()).get("comfort").get("label").asString();
        assertThat(before).isNotEqualTo("FEW_OPINIONS");
        send("PUT", "/api/v1/me/saved-places/" + placeId, bea);

        // Opiniones de noche muy incómodas hasta que la etiqueta cambie.
        for (String author : new String[] {ana, cami, dani}) {
            assertThat(post(placeId, author, BAD_NIGHT).statusCode()).isEqualTo(201);
        }
        HttpResponse<String> response = send("GET", "/api/v1/me/alerts", bea);
        JsonNode inbox = json.readTree(response.body());

        assertThat(inbox.get("unreadCount").asInt()).isEqualTo(1);
        JsonNode alert = inbox.get("items").get(0);
        assertThat(alert.get("placeId").asString()).isEqualTo(placeId.toString());
        assertThat(alert.get("previousLabel").asString()).isEqualTo(before);
        assertThat(alert.get("newLabel").asString()).isNotIn(before, "FEW_OPINIONS");
        assertThat(response.body()).doesNotContain("uid-ana").doesNotContain("uid-cami").doesNotContain("uid-dani");
        // Quien opinó y no guardó el lugar no recibe nada.
        assertThat(json.readTree(send("GET", "/api/v1/me/alerts", cami).body()).get("unreadCount").asInt()).isZero();

        assertThat(send("PUT", "/api/v1/me/alerts/read", bea).statusCode()).isEqualTo(204);
        JsonNode afterRead = json.readTree(send("GET", "/api/v1/me/alerts", bea).body());
        assertThat(afterRead.get("unreadCount").asInt()).isZero();
        assertThat(afterRead.get("items").get(0).get("read").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("Al eliminar la cuenta se borran sus lugares guardados y sus avisos")
    void deletingAccountRemovesSavedPlacesAndAlerts() throws Exception {
        insertGoodHistory(placeId, 4);
        send("PUT", "/api/v1/me/saved-places/" + placeId, bea);
        for (String author : new String[] {ana, cami, dani}) {
            post(placeId, author, BAD_NIGHT);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM alerts WHERE user_id = 'uid-bea'", Integer.class)).isEqualTo(1);

        assertThat(send("DELETE", "/api/v1/me", bea).statusCode()).isEqualTo(204);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM saved_places WHERE user_id = 'uid-bea'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM alerts WHERE user_id = 'uid-bea'", Integer.class)).isZero();
    }

    // Opiniones anteriores muy buenas, de día y de noche, sin autora registrada: dan evidencia suficiente.
    private void insertGoodHistory(UUID place, int count) {
        for (int i = 0; i < count; i++) {
            jdbc.update("""
                    INSERT INTO reviews (id, place_id, lighting, women_presence, commercial_visibility, night_experience, visit_period, created_at)
                    VALUES (gen_random_uuid(), ?, 5, 5, 5, 5, 'NIGHT', now() - make_interval(hours => ?))
                    """, place, i + 1);
        }
    }

    private UUID insertPlace(String name) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO places (id, name, category, location)
                VALUES (?, ?, 'PARK', ST_SetSRID(ST_MakePoint(-77.2811, 1.2136), 4326)::geography)
                """, id, name);
        return id;
    }

    private HttpResponse<String> post(UUID place, String bearer, String body) throws Exception {
        return http.send(request("/api/v1/places/" + place + "/reviews", bearer)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> send(String method, String path, String bearer) throws Exception {
        return http.send(request(path, bearer).method(method, HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path, String bearer) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return builder;
    }
}
