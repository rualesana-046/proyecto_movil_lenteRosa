package com.lenterosa.api.places.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GeoPointTest {

    @ParameterizedTest(name = "lat={0}, lng={1} se acepta")
    @CsvSource({"-90, -180", "90, 180", "0, 0", "1.2136, -77.2811"})
    @DisplayName("Los límites y un punto de Pasto se aceptan")
    void acceptsValidCoordinates(double latitude, double longitude) {
        GeoPoint point = new GeoPoint(latitude, longitude);

        assertThat(point.latitude()).isEqualTo(latitude);
        assertThat(point.longitude()).isEqualTo(longitude);
    }

    @ParameterizedTest(name = "latitud {0} se rechaza")
    @CsvSource({"-90.0001", "90.0001", "NaN", "Infinity"})
    @DisplayName("Una latitud fuera de -90 a 90 se rechaza")
    void rejectsInvalidLatitude(double latitude) {
        assertThatIllegalArgumentException().isThrownBy(() -> new GeoPoint(latitude, 0));
    }

    // Pasto invertido (-77.28, 1.21) es un punto válido cerca de la Antártida: el dominio no puede
    // detectarlo. Lo protege la prueba de persistencia que verifica el orden longitud/latitud en PostGIS.
    @ParameterizedTest(name = "longitud {0} se rechaza")
    @CsvSource({"-180.0001", "180.0001", "NaN", "-Infinity"})
    @DisplayName("Una longitud fuera de -180 a 180 se rechaza")
    void rejectsInvalidLongitude(double longitude) {
        assertThatIllegalArgumentException().isThrownBy(() -> new GeoPoint(0, longitude));
    }
}
