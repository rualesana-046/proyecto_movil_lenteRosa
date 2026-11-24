package com.lenterosa.api.alerts.domain;

import static com.lenterosa.api.comfortindex.ComfortLabel.COMFORTABLE;
import static com.lenterosa.api.comfortindex.ComfortLabel.FEW_OPINIONS;
import static com.lenterosa.api.comfortindex.ComfortLabel.MIXED_OPINIONS;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ComfortChangeTest {

    @Test
    @DisplayName("Un cambio de etiqueta siempre avisa, también al dejar de tener pocas opiniones")
    void labelChangeIsRelevant() {
        assertThat(ComfortChange.isRelevant(new ComfortSnapshot(COMFORTABLE, 66), new ComfortSnapshot(MIXED_OPINIONS, 58))).isTrue();
        assertThat(ComfortChange.isRelevant(new ComfortSnapshot(FEW_OPINIONS, null), new ComfortSnapshot(COMFORTABLE, 70))).isTrue();
    }

    @Test
    @DisplayName("Con la misma etiqueta, avisa solo si el índice se mueve 10 puntos o más")
    void indexMoveOfTenIsRelevant() {
        assertThat(ComfortChange.isRelevant(new ComfortSnapshot(COMFORTABLE, 60), new ComfortSnapshot(COMFORTABLE, 70))).isTrue();
        assertThat(ComfortChange.isRelevant(new ComfortSnapshot(COMFORTABLE, 70), new ComfortSnapshot(COMFORTABLE, 61))).isFalse();
    }

    @Test
    @DisplayName("Sin índice y con la misma etiqueta no hay aviso")
    void noIndexSameLabelIsNotRelevant() {
        assertThat(ComfortChange.isRelevant(new ComfortSnapshot(FEW_OPINIONS, null), new ComfortSnapshot(FEW_OPINIONS, 55))).isFalse();
    }
}
