package com.lenterosa.app.places

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lenterosa.app.R

object StaleTags {
    const val BANNER = "stale_banner"
}

/** Antigüedad legible: "hace 3 horas", "hace 2 días", "hace un momento". */
object Age {
    enum class Unit { MOMENT, MINUTES, HOURS, DAYS }

    data class Value(val amount: Int, val unit: Unit)

    fun of(savedAtMillis: Long, nowMillis: Long): Value {
        val minutes = ((nowMillis - savedAtMillis).coerceAtLeast(0) / 60_000).toInt()
        return when {
            minutes < 1 -> Value(0, Unit.MOMENT)
            minutes < 60 -> Value(minutes, Unit.MINUTES)
            minutes < 24 * 60 -> Value(minutes / 60, Unit.HOURS)
            else -> Value(minutes / (24 * 60), Unit.DAYS)
        }
    }
}

/**
 * Aviso de datos guardados (RF-10): dice cuándo se guardaron y por qué no se pudieron actualizar.
 * Nunca se muestran datos viejos como si fueran actuales.
 */
@Composable
fun StaleBanner(stale: Staleness, onRetry: () -> Unit, modifier: Modifier = Modifier, nowMillis: Long = System.currentTimeMillis()) {
    val age = Age.of(stale.savedAtMillis, nowMillis)
    val ago = when (age.unit) {
        Age.Unit.MOMENT -> stringResource(R.string.offline_age_moment)
        Age.Unit.MINUTES -> pluralStringResource(R.plurals.offline_age_minutes, age.amount, age.amount)
        Age.Unit.HOURS -> pluralStringResource(R.plurals.offline_age_hours, age.amount, age.amount)
        Age.Unit.DAYS -> pluralStringResource(R.plurals.offline_age_days, age.amount, age.amount)
    }
    val reason = stringResource(
        when (stale.reason) {
            LoadFailure.NETWORK -> R.string.offline_reason_network
            else -> R.string.offline_reason_server
        },
    )
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth().testTag(StaleTags.BANNER),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.offline_saved_ago, ago, reason),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            TextButton(onClick = onRetry) { Text(stringResource(R.string.places_retry)) }
        }
    }
}
