package com.lenterosa.api.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lenterosa.api.PostgisTestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// Sin FIREBASE_PROJECT_ID (como en las pruebas): la consulta pública funciona y todo lo demás pide sesión.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgisTestcontainersConfiguration.class)
class ApiSecurityIntegrationTest {

    @Value("${local.server.port}")
    int port;

    @Autowired
    JwtDecoder jwtDecoder;

    private final HttpClient http = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    @DisplayName("La consulta como invitada y la salud siguen siendo públicas")
    void guestEndpointsArePublic() throws Exception {
        assertThat(send("GET", "/api/v1/health", null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/v1/places?lat=1.2136&lng=-77.2811", null).statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("Sin sesión, una ruta protegida responde 401 con ProblemDetail y un mensaje claro")
    void protectedRouteWithoutTokenIsUnauthorized() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/places", null);
        JsonNode body = json.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate")).hasValue("Bearer");
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(body.get("detail").asString()).isEqualTo("Necesitas iniciar sesión para hacer esto.");
    }

    @Test
    @DisplayName("Sin FIREBASE_PROJECT_ID se rechaza cualquier token, aunque parezca válido")
    void missingProjectIdRejectsEveryToken() throws Exception {
        assertThatThrownBy(() -> jwtDecoder.decode("cualquier.token.firmado")).isInstanceOf(BadJwtException.class);
        assertThat(send("POST", "/api/v1/places", "cualquier.token.firmado").statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> send(String method, String path, String bearer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody());
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
