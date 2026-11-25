package com.lenterosa.app.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.lenterosa.app.R
import com.lenterosa.app.places.LoadState
import com.lenterosa.app.places.PlaceFormat
import com.lenterosa.app.places.PlaceSummary
import com.lenterosa.app.places.StaleBanner
import com.lenterosa.app.places.message
import com.lenterosa.app.saved.SaveButton
import com.lenterosa.app.ui.comfort.ComfortBadge
import com.lenterosa.app.ui.comfort.ComfortLevel
import com.lenterosa.app.ui.glass.GlassCardShape
import com.lenterosa.app.ui.glass.currentMotion
import com.lenterosa.app.ui.glass.glassSurface
import dev.chrisbanes.haze.HazeState

/** Etiquetas para las pruebas de UI de la pestaña del mapa. */
object MapTags {
    const val MAP = "map"
    const val ATTRIBUTION = "map_attribution"
    const val NEAR_ME = "map_near_me"
    const val SEARCH_HERE = "map_search_here"
    const val PLACE_CARD = "map_place_card"
    const val CONTRIBUTE = "map_contribute"
    const val STATUS = "map_status"
}

/**
 * Pestaña del mapa: el mapa ocupa el espacio disponible y los controles flotan encima. La atribución
 * no va aquí: va fuera de esta zona, junto a la barra inferior, para que nada la tape.
 */
@Composable
fun MapTab(
    state: MapUiState,
    contentPadding: PaddingValues,
    onNearMe: () -> Unit,
    onSearchHere: () -> Unit,
    onRetry: () -> Unit,
    onDismissLocationMessage: () -> Unit,
    onOpenPlace: (String) -> Unit,
    onContribute: (PlaceSummary) -> Unit,
    onCloseCard: () -> Unit,
    map: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    savedIds: Set<String> = emptySet(),
    onToggleSaved: (PlaceSummary) -> Unit = {},
    online: Boolean = true,
) {
    // Sin desenfoque sobre el mapa (su vista nativa no se puede capturar): las tarjetas usan el tinte.
    val hazeState: HazeState? = null
    val motion = currentMotion()
    // El mapa pasa por detrás de la barra superior de vidrio, pero termina encima de la atribución.
    Box(modifier.fillMaxSize().padding(bottom = contentPadding.calculateBottomPadding())) {
        map(Modifier.fillMaxSize().testTag(MapTags.MAP))
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = contentPadding.calculateTopPadding())
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Sin estado deshabilitado: sobre el mapa se vería transparente. El ViewModel ignora toques repetidos.
                Button(onClick = onNearMe, modifier = Modifier.testTag(MapTags.NEAR_ME)) {
                    if (state.locating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(painterResource(R.drawable.ic_near_me), contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = stringResource(if (state.locating) R.string.map_locating else R.string.map_near_me),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                AnimatedVisibility(visible = state.canSearchHere, enter = motion.popIn(), exit = motion.popOut()) {
                    FilledTonalButton(onClick = onSearchHere, modifier = Modifier.testTag(MapTags.SEARCH_HERE)) {
                        Text(stringResource(R.string.map_search_here))
                    }
                }
            }
            MapStatus(state, hazeState, onRetry, onDismissLocationMessage)
        }
        // Se recuerda el último lugar para que la tarjeta pueda salir animada después de cerrarla.
        var shownPlace by remember { mutableStateOf(state.selected) }
        if (state.selected != null) shownPlace = state.selected
        AnimatedVisibility(
            visible = state.selected != null,
            enter = motion.riseIn(),
            exit = motion.sinkOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            shownPlace?.let { place ->
                PlaceMapCard(
                    place = place,
                    saved = place.id in savedIds,
                    online = online,
                    onToggleSaved = { onToggleSaved(place) },
                    hazeState = hazeState,
                    onOpen = { onOpenPlace(place.id) },
                    onContribute = { onContribute(place) },
                    onClose = onCloseCard,
                    modifier = Modifier.padding(12.dp).widthIn(max = 560.dp).testTag(MapTags.PLACE_CARD),
                )
            }
        }
    }
}

@Composable
private fun MapStatus(state: MapUiState, hazeState: HazeState?, onRetry: () -> Unit, onDismissLocationMessage: () -> Unit) {
    val message = state.locationMessage
    when {
        message != null -> StatusCard(
            text = stringResource(
                when (message) {
                    LocationMessage.PERMISSION_DENIED -> R.string.map_location_denied
                    LocationMessage.DISABLED -> R.string.map_location_disabled
                    LocationMessage.UNAVAILABLE -> R.string.map_location_unavailable
                },
            ),
            action = stringResource(R.string.map_ok),
            onAction = onDismissLocationMessage,
            hazeState = hazeState,
        )
        state.places is LoadState.Error -> StatusCard(
            text = stringResource(state.places.failure.message()),
            action = stringResource(R.string.places_retry),
            onAction = onRetry,
            hazeState = hazeState,
        )
        state.places is LoadState.Loading -> Box(Modifier.glassSurface(hazeState, GlassCardShape)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.map_loading), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
            }
        }
        state.places is LoadState.Ready && state.places.stale != null ->
            StaleBanner(state.places.stale, onRetry = onRetry, modifier = Modifier.widthIn(max = 480.dp))
        state.places is LoadState.Ready && state.places.value.isEmpty() -> StatusCard(
            text = stringResource(R.string.map_no_places),
            action = null,
            onAction = {},
            hazeState = hazeState,
        )
    }
}

@Composable
private fun StatusCard(text: String, action: String?, onAction: () -> Unit, hazeState: HazeState?) {
    Card(
        modifier = Modifier.widthIn(max = 480.dp).glassSurface(hazeState, GlassCardShape).testTag(MapTags.STATUS),
        shape = GlassCardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (action != null) {
                TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) { Text(action) }
            }
        }
    }
}

@Composable
internal fun PlaceMapCard(
    place: PlaceSummary,
    saved: Boolean,
    online: Boolean,
    onToggleSaved: () -> Unit,
    hazeState: HazeState?,
    onOpen: () -> Unit,
    onContribute: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().glassSurface(hazeState, GlassCardShape),
        shape = GlassCardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    place.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                SaveButton(saved = saved, onToggle = onToggleSaved)
            }
            Text(
                text = stringResource(
                    R.string.places_category_distance,
                    stringResource(PlaceFormat.category(place.category)),
                    PlaceFormat.distance(place.distanceMeters),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ComfortBadge(ComfortLevel.fromCode(place.comfortLabel), place.comfortIndex)
            if (!online) {
                Text(stringResource(R.string.offline_contribute_needs_connection), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onOpen) { Text(stringResource(R.string.map_open_place)) }
                // Sin conexión, Aportar queda desactivado y se explica por qué.
                OutlinedButton(onClick = onContribute, enabled = online, modifier = Modifier.testTag(MapTags.CONTRIBUTE)) {
                    Text(stringResource(R.string.map_contribute))
                }
                TextButton(onClick = onClose, colors = ButtonDefaults.textButtonColors()) { Text(stringResource(R.string.map_close)) }
            }
        }
    }
}

/**
 * Atribución del proveedor del mapa (© OpenFreeMap, © OpenMapTiles, © OpenStreetMap), siempre
 * visible bajo el mapa y sobre la barra inferior, nunca encima del mapa ni de los controles.
 */
@Composable
fun MapAttribution(modifier: Modifier = Modifier) {
    val linkStyle = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline))
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier.fillMaxWidth().testTag(MapTags.ATTRIBUTION)) {
        Text(
            text = buildAnnotatedString {
                withLink(LinkAnnotation.Url(MapConfig.attributionUrl, linkStyle)) { append(MapConfig.attribution) }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
