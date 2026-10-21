package com.lenterosa.api.places.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlaceTest {

    private static final GeoPoint PASTO = new GeoPoint(1.2136, -77.2811);

    @Test
    @DisplayName("Nombre y dirección se guardan sin espacios sobrantes")
    void stripsNameAndAddress() {
        Place place = place("  Parque de prueba  ", "  Calle 1  ");

        assertThat(place.name()).isEqualTo("Parque de prueba");
        assertThat(place.address()).isEqualTo("Calle 1");
    }

    @ParameterizedTest(name = "dirección \"{0}\" queda ausente")
    @ValueSource(strings = {"", "   "})
    @DisplayName("Una dirección en blanco queda ausente")
    void blankAddressBecomesNull(String address) {
        assertThat(place("Parque", address).address()).isNull();
    }

    @ParameterizedTest(name = "nombre \"{0}\" se rechaza")
    @ValueSource(strings = {"", "   "})
    @DisplayName("Un nombre en blanco se rechaza")
    void rejectsBlankName(String name) {
        assertThatIllegalArgumentException().isThrownBy(() -> place(name, null));
    }

    @Test
    @DisplayName("Un nombre de más de 120 caracteres se rechaza")
    void rejectsTooLongName() {
        assertThatIllegalArgumentException().isThrownBy(() -> place("a".repeat(121), null));
    }

    @Test
    @DisplayName("Una dirección de más de 200 caracteres se rechaza")
    void rejectsTooLongAddress() {
        assertThatIllegalArgumentException().isThrownBy(() -> place("Parque", "a".repeat(201)));
    }

    private static Place place(String name, String address) {
        return new Place(UUID.randomUUID(), name, PlaceCategory.PARK, address, PASTO, Instant.now());
    }
}
