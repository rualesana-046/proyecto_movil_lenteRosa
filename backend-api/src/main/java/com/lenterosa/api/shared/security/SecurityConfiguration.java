package com.lenterosa.api.shared.security;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la API.
 *
 * <ul>
 *   <li>La consulta como invitada (GET de lugares y opiniones) y la salud son públicas.</li>
 *   <li>Todo lo demás exige un ID token de Firebase válido en {@code Authorization: Bearer}.</li>
 *   <li>Sin sesiones ni cookies: no hace falta protección CSRF.</li>
 *   <li>La API no tiene secretos de Firebase: valida la firma con las llaves públicas de Google.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/health", "/api/v1/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/places", "/api/v1/places/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(unauthorized()))
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(unauthorized()));
        return http.build();
    }

    /**
     * Decodificador de ID tokens de Firebase. Sin {@code FIREBASE_PROJECT_ID} rechaza todos los
     * tokens: si falta la configuración, nadie queda autenticado por error.
     */
    @Bean
    JwtDecoder firebaseJwtDecoder(@Value("${lente-rosa.firebase.project-id:}") String projectId, Clock clock) {
        if (projectId.isBlank()) {
            log.warn("FIREBASE_PROJECT_ID no está configurado: la API rechazará todos los tokens.");
            return token -> {
                throw new BadJwtException("Firebase no está configurado");
            };
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(FirebaseTokenValidators.JWK_SET_URI).build();
        decoder.setJwtValidator(FirebaseTokenValidators.forProject(projectId, clock));
        return decoder;
    }

    // 401 en formato RFC 9457 con un mensaje en lenguaje cotidiano. Nunca se describe por qué falló el token.
    private static AuthenticationEntryPoint unauthorized() {
        return (request, response, exception) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setHeader("WWW-Authenticate", "Bearer");
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("""
                    {"type":"about:blank","title":"Sesión requerida","status":401,\
                    "detail":"Necesitas iniciar sesión para hacer esto."}""");
        };
    }
}
