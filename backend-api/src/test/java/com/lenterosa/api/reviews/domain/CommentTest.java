package com.lenterosa.api.reviews.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class CommentTest {

    @ParameterizedTest(name = "\"{0}\" queda sin comentario")
    @NullSource
    @ValueSource(strings = {"", "   ", "\n\t"})
    @DisplayName("Un comentario nulo o en blanco queda sin comentario")
    void blankBecomesNull(String text) {
        assertThat(Comment.ofNullable(text)).isNull();
    }

    @Test
    @DisplayName("El comentario se guarda sin espacios sobrantes")
    void stripsText() {
        assertThat(Comment.ofNullable("  Buena iluminación  ").text()).isEqualTo("Buena iluminación");
    }

    @Test
    @DisplayName("500 caracteres se aceptan y 501 se rechazan")
    void enforcesMaxLength() {
        assertThat(Comment.ofNullable("a".repeat(500)).text()).hasSize(500);
        assertThatIllegalArgumentException().isThrownBy(() -> Comment.ofNullable("a".repeat(501)));
    }
}
