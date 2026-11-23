package com.lenterosa.api;

import com.lenterosa.api.shared.security.FirebaseTokenValidators;
import java.time.Clock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Decodificador para las pruebas: verifica la firma con la llave pública de {@link FirebaseTestTokens}
 * y aplica exactamente las mismas reglas de validación que en producción.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FirebaseTestSecurityConfiguration {

    @Bean
    @Primary
    JwtDecoder testFirebaseJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(FirebaseTestTokens.publicKey()).build();
        decoder.setJwtValidator(FirebaseTokenValidators.forProject(FirebaseTestTokens.PROJECT_ID, Clock.systemUTC()));
        return decoder;
    }
}
