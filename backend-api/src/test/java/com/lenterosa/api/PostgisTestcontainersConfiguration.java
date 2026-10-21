package com.lenterosa.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostGIS para las pruebas de integración.
 *
 * <p>Un único contenedor por ejecución de pruebas, compartido por todos los contextos de Spring:
 * en Apple Silicon la imagen corre emulada y arrancarla varias veces haría lentas las pruebas.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgisTestcontainersConfiguration {

    private static final PostgreSQLContainer POSTGIS = new PostgreSQLContainer(
            DockerImageName.parse("postgis/postgis:18-3.6").asCompatibleSubstituteFor("postgres"));

    static {
        POSTGIS.start();
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgisContainer() {
        return POSTGIS;
    }
}
