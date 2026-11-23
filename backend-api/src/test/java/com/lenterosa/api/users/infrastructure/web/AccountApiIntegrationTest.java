package com.lenterosa.api.users.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lenterosa.api.FirebaseTestSecurityConfiguration;
import com.lenterosa.api.FirebaseTestTokens;
import com.lenterosa.api.PostgisTestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Pruebas de punta a punta por HTTP real con tokens firmados como los de Firebase.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgisTestcontainersConfiguration.class, FirebaseTestSecurityConfiguration.class})
class AccountApiIntegrationTest {

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
        jdbc.update("DELETE FROM users");
    }

    @Test
    @DisplayName("Con un token válido la cuenta se registra una sola vez y se puede consultar")
    void registerIsIdempotent() throws Exception {
        String token = FirebaseTestTokens.valid("uid-ana", true);

        JsonNode first = json.readTree(send("PUT", token).body());
        JsonNode second = json.readTree(send("PUT", token).body());
        HttpResponse<String> current = send("GET", token);

        assertThat(first.get("uid").asString()).isEqualTo("uid-ana");
        assertThat(first.get("emailVerified").asBoolean()).isTrue();
        assertThat(second.get("registeredAt").asString()).isEqualTo(first.get("registeredAt").asString());
        assertThat(current.statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("Sin registrar, consultar la cuenta responde 404 con ProblemDetail")
    void unregisteredAccountIsNotFound() throws Exception {
        HttpResponse<String> response = send("GET", FirebaseTestTokens.valid("uid-nueva", false));

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(json.readTree(response.body()).get("title").asString()).isEqualTo("Cuenta no registrada");
    }

    @Test
    @DisplayName("Privacidad: el nombre o apodo del token no se guarda ni se devuelve; la tabla solo tiene uid y fecha")
    void displayNameIsNeverStored() throws Exception {
        String token = FirebaseTestTokens.valid("uid-con-apodo", true, claims -> claims
                .claim("name", "Apodo de prueba")
                .claim("email", "correo@ejemplo.com"));

        HttpResponse<String> response = send("PUT", token);

        assertThat(response.body()).doesNotContain("Apodo de prueba").doesNotContain("correo@ejemplo.com");
        List<String> columns = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'users' ORDER BY column_name",
                String.class);
        assertThat(columns).containsExactly("created_at", "id");
    }

    @Test
    @DisplayName("Eliminar la cuenta deja anónimas sus opiniones, borra el registro y no toca las de otras")
    void deleteAnonymizesReviewsAndRemovesAccount() throws Exception {
        String token = FirebaseTestTokens.valid("uid-a-borrar", true);
        send("PUT", token);
        UUID place = insertPlace();
        insertReview(place, "uid-a-borrar");
        insertReview(place, "uid-a-borrar");
        insertReview(place, "uid-otra");

        HttpResponse<String> response = send("DELETE", token);

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users WHERE id = 'uid-a-borrar'", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reviews", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reviews WHERE author_id IS NULL", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reviews WHERE author_id = 'uid-otra'", Integer.class))
                .isEqualTo(1);
        assertThat(send("DELETE", token).statusCode()).as("eliminar dos veces no es un error").isEqualTo(204);
    }

    @Test
    @DisplayName("Tokens inválidos responden 401: otra llave, vencido, otro proyecto o sin token")
    void invalidTokensAreRejected() throws Exception {
        assertThat(send("PUT", FirebaseTestTokens.signedWithOtherKey("uid-x")).statusCode()).isEqualTo(401);
        assertThat(send("PUT", FirebaseTestTokens.valid("uid-x", true, claims -> claims
                .issueTime(Date.from(Instant.now().minusSeconds(7_200)))
                .expirationTime(Date.from(Instant.now().minusSeconds(3_600))))).statusCode()).isEqualTo(401);
        assertThat(send("PUT", FirebaseTestTokens.valid("uid-x", true, claims -> claims.audience("otro-proyecto")))
                .statusCode()).isEqualTo(401);
        assertThat(send("DELETE", null).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Integer.class)).isZero();
    }

    private HttpResponse<String> send(String method, String bearer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/me"))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private UUID insertPlace() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO places (id, name, category, location)
                VALUES (?, 'Lugar de prueba', 'PARK', ST_SetSRID(ST_MakePoint(-77.2811, 1.2136), 4326)::geography)
                """, id);
        return id;
    }

    private void insertReview(UUID placeId, String authorId) {
        jdbc.update("""
                INSERT INTO reviews (id, place_id, author_id, lighting, women_presence, commercial_visibility,
                                     night_experience, visit_period)
                VALUES (?, ?, ?, 3, 3, 3, 3, 'NIGHT')
                """, UUID.randomUUID(), placeId, authorId);
    }
}
