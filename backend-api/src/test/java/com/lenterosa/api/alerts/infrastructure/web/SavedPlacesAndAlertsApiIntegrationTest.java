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
    @DisplayName("Cuando una opinión cambia la etiqueta de un lugar guardado, quien lo guardó recibe un aviso sin saber quién opinó")
    void relevantChangeCreatesAlert() throws Exception {
        send("PUT", "/api/v1/me/saved-places/" + placeId, bea);

        // Con dos opiniones recién publicadas la evidencia es apenas menor que 2 (por el peso por
        // antigüedad), así que el lugar sigue con pocas opiniones: no hay aviso.
        assertThat(post(placeId, ana, GOOD_DAY).statusCode()).isEqualTo(201);
        assertThat(post(placeId, dani, GOOD_DAY).statusCode()).isEqualTo(201);
        assertThat(json.readTree(send("GET", "/api/v1/me/alerts", bea).body()).get("unreadCount").asInt()).isZero();

        // La tercera opinión le da etiqueta al lugar: cambio relevante.
        assertThat(post(placeId, cami, GOOD_DAY).statusCode()).isEqualTo(201);
        HttpResponse<String> response = send("GET", "/api/v1/me/alerts", bea);
        JsonNode inbox = json.readTree(response.body());

        assertThat(inbox.get("unreadCount").asInt()).isEqualTo(1);
        JsonNode alert = inbox.get("items").get(0);
        assertThat(alert.get("placeId").asString()).isEqualTo(placeId.toString());
        assertThat(alert.get("previousLabel").asString()).isEqualTo("FEW_OPINIONS");
        assertThat(alert.get("newLabel").asString()).isNotEqualTo("FEW_OPINIONS");
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
        send("PUT", "/api/v1/me/saved-places/" + placeId, bea);
        post(placeId, ana, GOOD_DAY);
        post(placeId, dani, GOOD_DAY);
        post(placeId, cami, GOOD_DAY);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM alerts WHERE user_id = 'uid-bea'", Integer.class)).isEqualTo(1);

        assertThat(send("DELETE", "/api/v1/me", bea).statusCode()).isEqualTo(204);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM saved_places WHERE user_id = 'uid-bea'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM alerts WHERE user_id = 'uid-bea'", Integer.class)).isZero();
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
