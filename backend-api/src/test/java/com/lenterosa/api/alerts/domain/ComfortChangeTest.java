package com.lenterosa.api.alerts.domain;

import static com.lenterosa.api.comfortindex.ComfortLabel.COMFORTABLE;
import static com.lenterosa.api.comfortindex.ComfortLabel.FEW_OPINIONS;
import static com.lenterosa.api.comfortindex.ComfortLabel.MIXED_OPINIONS;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ComfortChangeTest {

    @Test
    @DisplayName("Con evidencia suficiente, un cambio de etiqueta avisa")
    void labelChangeIsRelevant() {
        assertThat(ComfortChange.isRelevant(new ComfortSnapshot(COMFORTABLE, 66), new ComfortSnapshot(MIXED_OPINIONS, 58))).isTrue();
    }

    @Test
    @DisplayName("Mientras el lugar esté en Pocas opiniones, antes o después, no hay aviso")
    void fewOpinionsNeverNotify() {
        assertThat(ComfortChange.isRelevant(new ComfortSnapshot(FEW_OPINIONS, null), new ComfortSnapshot(COMFORTABLE, 70))).isFalse();
        assertThat(ComfortChange.isRelevant(new ComfortSnapshot(COMFORTABLE, 70), new ComfortSnapshot(FEW_OPINIONS, 40))).isFalse();
        assertThat(ComfortChange.isRelevant(new ComfortSnapshot(FEW_OPINIONS, 30), new ComfortSnapshot(FEW_OPINIONS, 80))).isFalse();
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
