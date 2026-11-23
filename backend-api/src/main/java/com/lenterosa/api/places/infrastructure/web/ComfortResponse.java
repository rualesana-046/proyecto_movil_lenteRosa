package com.lenterosa.api.places.infrastructure.web;

import com.lenterosa.api.comfortindex.ComfortIndex;
import com.lenterosa.api.comfortindex.DimensionComfort;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Índice de Confort en el JSON. Las etiquetas van como códigos en inglés; la app muestra el texto
 * en español. Los promedios bayesianos se redondean a un decimal y las medidas de incertidumbre a dos.
 */
record ComfortResponse(
        Integer index,
        String label,
        String uncertainty,
        double effectiveReviews,
        double dispersion,
        boolean nightDataMissing,
        DimensionComfort dimensions,
        int formulaVersion) {

    static ComfortResponse from(ComfortIndex comfort) {
        DimensionComfort dimensions = comfort.dimensions();
        return new ComfortResponse(
                comfort.index(),
                comfort.label().name(),
                comfort.uncertainty().name(),
                round(comfort.effectiveReviews(), 2),
                round(comfort.dispersion(), 2),
                comfort.nightDataMissing(),
                new DimensionComfort(
                        round(dimensions.lighting(), 1),
                        round(dimensions.womenPresence(), 1),
                        round(dimensions.commercialVisibility(), 1),
                        round(dimensions.nightExperience(), 1)),
                comfort.formulaVersion());
    }

    private static Double round(Double value, int decimals) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
    }
}
