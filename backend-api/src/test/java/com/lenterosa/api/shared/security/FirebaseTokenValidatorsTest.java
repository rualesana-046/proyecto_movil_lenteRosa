package com.lenterosa.api.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

class FirebaseTokenValidatorsTest {

    private static final String PROJECT = "lente-rosa-test";
    private static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    private static final OAuth2TokenValidator<Jwt> VALIDATOR =
            FirebaseTokenValidators.forProject(PROJECT, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("Un token de Firebase del proyecto, vigente y con uid, es válido")
    void validTokenPasses() {
        assertThat(VALIDATOR.validate(token(builder -> { })).hasErrors()).isFalse();
    }

    @Test
    @DisplayName("Un emisor de otro proyecto se rechaza")
    void wrongIssuerFails() {
        assertThat(VALIDATOR.validate(token(b -> b.issuer(FirebaseTokenValidators.issuer("otro")))).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Una audiencia de otro proyecto se rechaza")
    void wrongAudienceFails() {
        assertThat(VALIDATOR.validate(token(b -> b.audience(List.of("otro")))).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Un uid vacío se rechaza")
    void blankSubjectFails() {
        assertThat(VALIDATOR.validate(token(b -> b.subject(" "))).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("Un auth_time futuro o ausente se rechaza")
    void authTimeMustBeInThePast() {
        assertThat(VALIDATOR.validate(token(b -> b.claim("auth_time", NOW.plusSeconds(3_600)))).hasErrors()).isTrue();
        assertThat(VALIDATOR.validate(token(b -> b.claims(c -> c.remove("auth_time")))).hasErrors()).isTrue();
    }

    private static Jwt token(Consumer<Jwt.Builder> customizer) {
        // Fechas relativas al reloj real: los validadores por defecto de Spring usan el reloj del sistema.
        Instant issuedAt = Instant.now().minusSeconds(60);
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer(FirebaseTokenValidators.issuer(PROJECT))
                .audience(List.of(PROJECT))
                .subject("uid-de-prueba")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(3_600))
                .claim("auth_time", NOW.minusSeconds(120));
        customizer.accept(builder);
        return builder.build();
    }
}
