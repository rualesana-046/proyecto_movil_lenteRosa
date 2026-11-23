package com.lenterosa.api.shared.security;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

/**
 * Reglas de validación de un ID token de Firebase Authentication, según la documentación de
 * Firebase para verificar tokens con una biblioteca JWT de terceros:
 *
 * <ul>
 *   <li>firma con las llaves públicas de Google (la comprueba el decodificador);</li>
 *   <li>{@code exp} en el futuro y {@code iat} en el pasado (validadores por defecto);</li>
 *   <li>{@code iss} = {@code https://securetoken.google.com/<projectId>};</li>
 *   <li>{@code aud} contiene el {@code projectId};</li>
 *   <li>{@code sub} no vacío (es el uid) y {@code auth_time} en el pasado.</li>
 * </ul>
 */
public final class FirebaseTokenValidators {

    public static final String JWK_SET_URI =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    private FirebaseTokenValidators() {
    }

    public static String issuer(String projectId) {
        return "https://securetoken.google.com/" + projectId;
    }

    public static OAuth2TokenValidator<Jwt> forProject(String projectId, Clock clock) {
        return new DelegatingOAuth2TokenValidator<>(List.of(
                JwtValidators.createDefaultWithIssuer(issuer(projectId)),
                new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(projectId)),
                new JwtClaimValidator<String>("sub", sub -> sub != null && !sub.isBlank() && sub.length() <= 128),
                authTimeInThePast(clock)));
    }

    private static OAuth2TokenValidator<Jwt> authTimeInThePast(Clock clock) {
        return jwt -> {
            Object authTime = jwt.getClaims().get("auth_time");
            Instant instant = switch (authTime) {
                case Instant value -> value;
                case Number seconds -> Instant.ofEpochSecond(seconds.longValue());
                case null, default -> null;
            };
            // Se tolera un minuto de diferencia entre relojes, como en los validadores de Spring.
            if (instant != null && !instant.isAfter(clock.instant().plusSeconds(60))) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "auth_time falta o está en el futuro", null));
        };
    }
}
