package com.lenterosa.app.ui.comfort

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lenterosa.app.R
import com.lenterosa.app.ui.theme.ComfortTone
import com.lenterosa.app.ui.theme.LenteRosaComfort

/**
 * Etiqueta del Índice de Confort (RF-03) en la app: texto en español, ícono y tono de la escala.
 * El significado nunca depende solo del color (RNF-02).
 */
enum class ComfortLevel(val tone: ComfortTone, @param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    VERY_COMFORTABLE(ComfortTone.VERY_COMFORTABLE, R.string.comfort_very_comfortable, R.drawable.ic_comfort_very),
    COMFORTABLE(ComfortTone.COMFORTABLE, R.string.comfort_comfortable, R.drawable.ic_comfort_good),
    MIXED_OPINIONS(ComfortTone.MIXED, R.string.comfort_mixed_opinions, R.drawable.ic_comfort_mixed),
    UNCOMFORTABLE(ComfortTone.UNCOMFORTABLE, R.string.comfort_uncomfortable, R.drawable.ic_comfort_low),
    FEW_OPINIONS(ComfortTone.FEW_OPINIONS, R.string.comfort_few_opinions, R.drawable.ic_comfort_few),
    ;

    companion object {
        /** Código de la API; uno desconocido se muestra como "Pocas opiniones", nunca como cómodo. */
        fun fromCode(code: String?): ComfortLevel = entries.firstOrNull { it.name == code } ?: FEW_OPINIONS
    }
}

/** Insignia con ícono, etiqueta y, si existe, el índice de 0 a 100. Se lee como un solo elemento. */
@Composable
fun ComfortBadge(level: ComfortLevel, index: Int?, modifier: Modifier = Modifier, large: Boolean = false) {
    val colors = LenteRosaComfort.colors(level.tone)
    val label = stringResource(level.label)
    val value = index?.let { stringResource(R.string.comfort_index_value, it) }
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) { }
            .background(colors.container, RoundedCornerShape(if (large) 16.dp else 12.dp))
            .padding(horizontal = if (large) 14.dp else 10.dp, vertical = if (large) 10.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(level.icon),
            contentDescription = null,
            tint = colors.content,
            modifier = Modifier.size(if (large) 24.dp else 18.dp),
        )
        Text(
            text = label,
            color = colors.content,
            style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
        )
        if (value != null) {
            Text(
                text = value,
                color = colors.content,
                style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
            )
        }
    }
}
