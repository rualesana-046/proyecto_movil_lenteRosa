package com.lenterosa.api.reviews.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RatingTest {

    @ParameterizedTest(name = "{0} se acepta")
    @ValueSource(ints = {1, 2, 3, 4, 5})
    @DisplayName("Los valores de 1 a 5 se aceptan")
    void acceptsOneToFive(int value) {
        assertThat(new Rating(value).value()).isEqualTo(value);
    }

    @ParameterizedTest(name = "{0} se rechaza")
    @ValueSource(ints = {0, 6, -1, 100})
    @DisplayName("Los valores fuera de 1 a 5 se rechazan")
    void rejectsOutOfRange(int value) {
        assertThatIllegalArgumentException().isThrownBy(() -> new Rating(value));
    }
}
