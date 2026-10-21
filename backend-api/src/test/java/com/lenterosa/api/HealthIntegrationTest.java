package com.lenterosa.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgisTestcontainersConfiguration.class)
class HealthIntegrationTest {

    @Value("${local.server.port}")
    int port;

    @Autowired
    JdbcTemplate jdbc;

    private final HttpClient http = HttpClient.newHttpClient();

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    @DisplayName("GET /api/v1/health responde 200 con status UP y sin componentes ni detalles")
    void healthRespondsUpOnApiV1() throws Exception {
        HttpResponse<String> response = get("/api/v1/health");
        JsonNode body = json.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.get("status").asString()).isEqualTo("UP");
        // Los grupos de sondas (liveness, readiness) se permiten; los componentes y detalles no.
        assertThat(body.has("components")).isFalse();
        assertThat(body.has("details")).isFalse();
    }

    @ParameterizedTest(name = "GET {0} responde 200 con status UP")
    @ValueSource(strings = {"/api/v1/health/liveness", "/api/v1/health/readiness"})
    @DisplayName("Las sondas de liveness y readiness responden 200 con status UP")
    void probesRespondUp(String path) throws Exception {
        HttpResponse<String> response = get(path);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.readTree(response.body()).get("status").asString()).isEqualTo("UP");
    }

    @Test
    @DisplayName("/actuator/health no está expuesto")
    void actuatorHealthIsNotExposed() throws Exception {
        assertThat(get("/actuator/health").statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("La migración de Flyway habilita PostGIS 3.6")
    void flywayEnablesPostgis() {
        String version = jdbc.queryForObject("SELECT postgis_lib_version()", String.class);

        assertThat(version).startsWith("3.6");
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
