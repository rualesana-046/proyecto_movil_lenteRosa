package com.lenterosa.app.saved

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenterosa.app.R
import com.lenterosa.app.places.Centered
import com.lenterosa.app.places.LoadErrorMessage
import com.lenterosa.app.places.PlaceDetail
import com.lenterosa.app.places.PlaceFormat
import com.lenterosa.app.ui.comfort.ComfortBadge
import com.lenterosa.app.ui.comfort.ComfortLevel

/** Pestaña Guardados: primero los avisos de cambio y después los lugares guardados. */
@Composable
fun SavedContent(
    viewModel: SavedViewModel,
    onOpenPlace: (String) -> Unit,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LaunchedEffect(Unit) { viewModel.load() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    SavedList(state, onOpenPlace, onOpenAccount, onRetry = viewModel::load, onRemove = viewModel::remove, modifier, contentPadding)
}

/** Contenido de la pestaña Guardados según su estado (nulo mientras carga). */
@Composable
fun SavedList(
    state: SavedLoad?,
    onOpenPlace: (String) -> Unit,
    onOpenAccount: () -> Unit,
    onRetry: () -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state) {
            null -> item { Centered { CircularProgressIndicator() } }
            SavedLoad.NotSignedIn -> item {
                Column(Modifier.fillMaxWidth().padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.saved_sign_in), style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = onOpenAccount, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.detail_go_to_account))
                    }
                }
            }
            is SavedLoad.Failed -> item { Centered { LoadErrorMessage(state.failure, onRetry = onRetry) } }
            is SavedLoad.Ready -> {
                val names = state.overview.places.associate { it.id to it.name }
                if (state.overview.inbox.items.isNotEmpty()) {
                    item { SectionTitle(R.string.saved_alerts_title) }
                    items(state.overview.inbox.items, key = { "alerta-" + it.id }) { alert ->
                        AlertCard(alert, names[alert.placeId], onClick = { onOpenPlace(alert.placeId) })
                    }
                }
                item { SectionTitle(R.string.saved_places_title) }
                if (state.overview.places.isEmpty()) {
                    item { Text(stringResource(R.string.saved_empty), style = MaterialTheme.typography.bodyMedium) }
                }
                items(state.overview.places, key = { it.id }) { place ->
                    SavedPlaceCard(place, onClick = { onOpenPlace(place.id) }, onRemove = { onRemove(place.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: Int) {
    Text(
        stringResource(text),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp).semantics { heading() },
    )
}

/** Aviso sutil: texto calmado y cotidiano, sin alarmas. "Nueva" se escribe, no solo se colorea. */
@Composable
private fun AlertCard(alert: AlertDto, placeName: String?, onClick: () -> Unit) {
    val now = stringResource(ComfortLevel.fromCode(alert.newLabel).label)
    val before = stringResource(ComfortLevel.fromCode(alert.previousLabel).label)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.places_open_detail), onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!alert.read) {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                        Text(
                            stringResource(R.string.saved_alert_new),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    placeName ?: stringResource(R.string.saved_alert_unknown_place),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (alert.read) FontWeight.Medium else FontWeight.Bold,
                )
            }
            Text(stringResource(R.string.saved_alert_change, now, before), style = MaterialTheme.typography.bodyMedium)
            ComfortBadge(ComfortLevel.fromCode(alert.newLabel), alert.newIndex)
            PlaceFormat.date(alert.createdAt)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SavedPlaceCard(place: PlaceDetail, onClick: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.places_open_detail), onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 4.dp)) {
            Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(place.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                Text(
                    stringResource(PlaceFormat.category(place.category)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ComfortBadge(ComfortLevel.fromCode(place.comfort.label), place.comfort.index)
            }
            SaveButton(saved = true, onToggle = onRemove)
        }
    }
}

/** Botón para guardar o quitar un lugar. La descripción dice la acción, no solo el estado. */
@Composable
fun SaveButton(saved: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            painterResource(if (saved) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border),
            contentDescription = stringResource(if (saved) R.string.saved_remove else R.string.saved_add),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Mensaje después de intentar guardar. */
@Composable
fun saveResultMessage(result: SaveResult): String? = when (result) {
    SaveResult.DONE -> null
    SaveResult.NOT_SIGNED_IN -> stringResource(R.string.saved_sign_in)
    SaveResult.LIMIT_REACHED -> stringResource(R.string.saved_limit)
    SaveResult.FAILED -> stringResource(R.string.saved_failed)
}
