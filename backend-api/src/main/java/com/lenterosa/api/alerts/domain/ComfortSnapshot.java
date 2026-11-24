package com.lenterosa.api.alerts.domain;

import com.lenterosa.api.comfortindex.ComfortLabel;
import java.util.Objects;

/** Etiqueta e índice (0 a 100, nulo sin opiniones) del Índice de Confort en un momento. */
public record ComfortSnapshot(ComfortLabel label, Integer index) {

    public ComfortSnapshot {
        Objects.requireNonNull(label, "label");
    }
}
