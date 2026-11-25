package com.lenterosa.api.alerts.domain;

import com.lenterosa.api.comfortindex.ComfortLabel;

/**
 * Cuándo un cambio del Índice de Confort merece un aviso (RF-07, ADR 0012).
 *
 * <p>Solo se compara cuando hay evidencia suficiente antes y después: si el lugar está en "Pocas
 * opiniones" en cualquiera de los dos momentos, no hay aviso. Con evidencia suficiente, se avisa si
 * cambia la etiqueta o si el índice se mueve al menos {@value #INDEX_THRESHOLD} puntos. Los cambios
 * pequeños no avisan: las alertas deben ser sutiles.
 */
public final class ComfortChange {

    public static final int INDEX_THRESHOLD = 10;

    private ComfortChange() {
    }

    public static boolean isRelevant(ComfortSnapshot before, ComfortSnapshot after) {
        if (before.label() == ComfortLabel.FEW_OPINIONS || after.label() == ComfortLabel.FEW_OPINIONS) {
            return false;
        }
        if (before.label() != after.label()) {
            return true;
        }
        if (before.index() == null || after.index() == null) {
            return false;
        }
        return Math.abs(after.index() - before.index()) >= INDEX_THRESHOLD;
    }
}
