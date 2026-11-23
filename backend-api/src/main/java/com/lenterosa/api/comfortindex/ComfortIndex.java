package com.lenterosa.api.comfortindex;

/**
 * Índice de Confort de un lugar. Refleja percepción comunitaria: no garantiza seguridad.
 *
 * @param index entero de 0 a 100; nulo si el lugar no tiene opiniones
 * @param effectiveReviews suma de los pesos por antigüedad de las opiniones
 * @param dispersion desviación estándar ponderada del promedio de cada opinión, en escala 1 a 5
 * @param nightDataMissing la evidencia sobre la experiencia nocturna es menor que una opinión reciente
 * @param formulaVersion versión de la fórmula con que se calculó, para poder verificarla
 */
public record ComfortIndex(
        Integer index,
        ComfortLabel label,
        Uncertainty uncertainty,
        double effectiveReviews,
        double dispersion,
        boolean nightDataMissing,
        DimensionComfort dimensions,
        int formulaVersion) {
}
