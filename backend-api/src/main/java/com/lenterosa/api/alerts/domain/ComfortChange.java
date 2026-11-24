package com.lenterosa.api.alerts.domain;

/**
 * Cuándo un cambio del Índice de Confort merece un aviso (RF-07, ADR 0012). Se avisa si cambia la
 * etiqueta o si el índice se mueve al menos {@value #INDEX_THRESHOLD} puntos. Los cambios pequeños
 * no avisan: las alertas deben ser sutiles.
 */
public final class ComfortChange {

    public static final int INDEX_THRESHOLD = 10;

    private ComfortChange() {
    }

    public static boolean isRelevant(ComfortSnapshot before, ComfortSnapshot after) {
        if (before.label() != after.label()) {
            return true;
        }
        if (before.index() == null || after.index() == null) {
            return false;
        }
        return Math.abs(after.index() - before.index()) >= INDEX_THRESHOLD;
    }
}
