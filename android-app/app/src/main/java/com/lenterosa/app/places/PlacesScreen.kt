package com.lenterosa.app.places

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenterosa.app.R
import com.lenterosa.app.ui.comfort.ComfortBadge
import com.lenterosa.app.ui.comfort.ComfortLevel

/** Lista de lugares cerca del centro del piloto, del más cercano al más lejano (RF-01). */
@Composable
fun PlacesContent(viewModel: PlacesViewModel, onOpenPlace: (String) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    Column(modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.places_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text(stringResource(R.string.places_search)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focus.clearFocus()
                viewModel.search()
            }),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        when (val places = state.places) {
            LoadState.Loading -> Centered { CircularProgressIndicator() }
            is LoadState.Error -> Centered {
                LoadErrorMessage(places.failure, onRetry = viewModel::search)
            }
            is LoadState.Ready -> if (places.value.isEmpty()) {
                Centered {
                    Text(
                        text = stringResource(if (state.query.isBlank()) R.string.places_empty else R.string.places_empty_search),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(places.value, key = { it.id }) { place -> PlaceCard(place, onClick = { onOpenPlace(place.id) }) }
                    item { Disclaimer(Modifier.padding(top = 8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun PlaceCard(place: PlaceSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.places_open_detail), onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(place.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.places_category_distance, stringResource(PlaceFormat.category(place.category)),
                        PlaceFormat.distance(place.distanceMeters)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ComfortBadge(ComfortLevel.fromCode(place.comfortLabel), place.comfortIndex)
        }
    }
}

@Composable
internal fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
internal fun LoadErrorMessage(failure: LoadFailure, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(failure.message()), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry) { Text(stringResource(R.string.places_retry)) }
    }
}

internal fun LoadFailure.message(): Int = when (this) {
    LoadFailure.NETWORK -> R.string.places_error_network
    LoadFailure.SERVER_UNREACHABLE -> R.string.places_error_unreachable
    LoadFailure.NOT_FOUND -> R.string.places_error_not_found
    LoadFailure.SERVER -> R.string.places_error_server
}

/** Aviso permanente: el índice refleja percepción comunitaria y no garantiza seguridad. */
@Composable
internal fun Disclaimer(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.comfort_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
