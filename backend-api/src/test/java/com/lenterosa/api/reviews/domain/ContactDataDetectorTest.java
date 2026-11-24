package com.lenterosa.api.reviews.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContactDataDetectorTest {

    @ParameterizedTest(name = "\"{0}\" se permite")
    @ValueSource(strings = {
        "Queda en la Calle 18 # 20-30, buena iluminación",
        "Carrera 27 #15-40 apto 302",
        "Cra. 27 # 15-40, frente al parque",
        "Diagonal 16B # 25-10",
        "Transversal 4 No. 12-35",
        "Avenida Boyacá con Calle 20",
        "Pasé a las 10 y a las 11 de la noche",
        "Le doy 4 de 5 por la iluminación",
        "Había unas 30 personas, en 2026 mejoró",
        "Ruta 15 o 16 del bus"
    })
    @DisplayName("Direcciones colombianas y números cortos no son datos de contacto")
    void addressesAndShortNumbersAreAllowed(String comment) {
        assertThat(ContactDataDetector.containsContactData(comment)).isFalse();
    }

    @ParameterizedTest(name = "\"{0}\" se detecta")
    @ValueSource(strings = {
        "Llámame al 3001234567",
        "Celular 300 123 4567",
        "Escribe al +57 300 123 4567",
        "+57-300-123-4567 para más información",
        "Teléfono fijo 601 234 5678",
        "Fijo 723 4567",
        "Marca 7234567",
        "Número 300.123.4567"
    })
    @DisplayName("Teléfonos de 7 o más dígitos, con espacios, guiones o +57, se detectan")
    void phonesAreDetected(String comment) {
        assertThat(ContactDataDetector.containsContactData(comment)).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" se detecta")
    @ValueSource(strings = {
        "Escríbeme a ana.perez+pasto@correo.com",
        "Mira https://ejemplo.org/lugar",
        "Más fotos en www.ejemplo.com",
        "Busca ejemplo.com.co/fotos",
        "Info en mi-negocio.co"
    })
    @DisplayName("Correos y enlaces se detectan")
    void emailsAndLinksAreDetected(String comment) {
        assertThat(ContactDataDetector.containsContactData(comment)).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" se permite")
    @ValueSource(strings = {"", "   "})
    @DisplayName("Un comentario vacío no tiene datos de contacto")
    void blankIsAllowed(String comment) {
        assertThat(ContactDataDetector.containsContactData(comment)).isFalse();
    }
}
