package com.lenterosa.app.places

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenterosa.app.R
import com.lenterosa.app.account.Session
import com.lenterosa.app.saved.SaveButton
import com.lenterosa.app.saved.saveResultMessage
import com.lenterosa.app.ui.comfort.ComfortBadge
import com.lenterosa.app.ui.comfort.ComfortLevel
import com.lenterosa.app.ui.glass.GlassTopBar
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

object DetailTags {
    const val CONTRIBUTE = "detail_contribute"
}

/** Ficha del lugar: índice, promedios por dimensión y opiniones recientes (RF-01, RF-03). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    viewModel: PlaceDetailViewModel,
    placeId: String,
    refreshKey: Long,
    onBack: () -> Unit,
    onContribute: (id: String, name: String) -> Unit,
    onOpenAccount: () -> Unit,
) {
    // `refreshKey` cambia al publicar una opinión: la ficha se recarga con el índice actualizado.
    LaunchedEffect(placeId, refreshKey) { viewModel.load(placeId, force = refreshKey > 0L) }
    val state by viewModel.place.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    val saveMessage by viewModel.saveMessage.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val messageText = saveMessage?.let { saveResultMessage(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            viewModel.dismissSaveMessage()
        }
    }
    val hazeState = rememberHazeState()
    Scaffold(
        // Sin título en la barra: el nombre del lugar ya es el encabezado de la ficha.
        topBar = {
            GlassTopBar(
                hazeState,
                navigationIcon = { BackButton(onBack) },
                actions = { SaveButton(saved = placeId in savedIds, onToggle = { viewModel.toggleSaved(placeId) }) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
            when (val current = state) {
                LoadState.Loading -> Column(Modifier.padding(padding)) { Centered { CircularProgressIndicator() } }
                is LoadState.Error -> Column(Modifier.padding(padding)) {
                    Centered { LoadErrorMessage(current.failure, onRetry = viewModel::retry) }
                }
                is LoadState.Ready -> PlaceDetailContent(current.value, current.stale, online, session, padding, onContribute,
                    onOpenAccount, onRetry = viewModel::retry)
            }
        }
    }
}

@Composable
internal fun PlaceDetailContent(
    data: PlaceWithReviews,
    stale: Staleness?,
    online: Boolean,
    session: Session?,
    padding: PaddingValues,
    onContribute: (id: String, name: String) -> Unit,
    onOpenAccount: () -> Unit,
    onRetry: () -> Unit,
) {
    val place = data.place
    // La lista pasa por detrás de la barra de vidrio; el relleno deja libre su espacio.
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 16.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        stale?.let { item { StaleBanner(it, onRetry = onRetry) } }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(place.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(
                    text = listOfNotNull(stringResource(PlaceFormat.category(place.category)), place.address).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { ComfortSection(place) }
        item { AveragesSection(place) }
        item { ContributeAction(place, session, online, onContribute, onOpenAccount) }
        item {
            Text(
                text = stringResource(R.string.detail_recent_reviews),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (data.reviews.isEmpty()) {
            item { Text(stringResource(R.string.detail_no_reviews), style = MaterialTheme.typography.bodyMedium) }
        }
        items(data.reviews, key = { it.id }) { ReviewCard(it) }
        item { Disclaimer() }
    }
}

@Composable
private fun ComfortSection(place: PlaceDetail) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.detail_comfort_title), style = MaterialTheme.typography.titleMedium)
            ComfortBadge(ComfortLevel.fromCode(place.comfort.label), place.comfort.index, large = true)
            Text(stringResource(uncertaintyText(place.comfort.uncertainty)), style = MaterialTheme.typography.bodyMedium)
            if (place.comfort.nightDataMissing) {
                Text(stringResource(R.string.detail_night_missing), style = MaterialTheme.typography.bodyMedium)
            }
            val count = place.reviewCount.toInt()
            val last = PlaceFormat.date(place.lastReviewAt)
            Text(
                text = if (last != null) {
                    pluralStringResource(R.plurals.detail_review_count_last, count, count, last)
                } else {
                    pluralStringResource(R.plurals.detail_review_count, count, count)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun uncertaintyText(code: String): Int = when (code) {
    "LOW" -> R.string.uncertainty_low
    "MEDIUM" -> R.string.uncertainty_medium
    else -> R.string.uncertainty_high
}

@Composable
private fun AveragesSection(place: PlaceDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.detail_averages_title), style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() })
        AverageRow(R.string.dimension_lighting, place.averages.lighting)
        AverageRow(R.string.dimension_women_presence, place.averages.womenPresence)
        AverageRow(R.string.dimension_commercial_visibility, place.averages.commercialVisibility)
        AverageRow(R.string.dimension_night_experience, place.averages.nightExperience)
    }
}

@Composable
private fun AverageRow(label: Int, value: Double?) {
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { }, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
        val formatted = PlaceFormat.average(value)
        Text(
            text = if (formatted != null) stringResource(R.string.detail_average_value, formatted) else stringResource(R.string.detail_no_data),
            style = MaterialTheme.typography.bodyLarge,
            color = if (formatted != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ContributeAction(
    place: PlaceDetail,
    session: Session?,
    online: Boolean,
    onContribute: (id: String, name: String) -> Unit,
    onOpenAccount: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            // Sin conexión no se puede opinar: las opiniones no se guardan para enviarlas después.
            !online -> {
                Text(stringResource(R.string.offline_contribute_needs_connection), style = MaterialTheme.typography.bodyMedium)
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().testTag(DetailTags.CONTRIBUTE)) {
                    Text(stringResource(R.string.detail_contribute))
                }
            }
            session == null -> {
                Text(stringResource(R.string.detail_sign_in_to_contribute), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onOpenAccount, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.detail_go_to_account))
                }
            }
            !session.emailVerified -> {
                Text(stringResource(R.string.detail_verify_to_contribute), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onOpenAccount, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.detail_go_to_account))
                }
            }
            else -> Button(onClick = { onContribute(place.id, place.name) }, modifier = Modifier.fillMaxWidth().testTag(DetailTags.CONTRIBUTE)) {
                Text(stringResource(R.string.detail_contribute))
            }
        }
    }
}

@Composable
private fun ReviewCard(review: Review) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val period = stringResource(if (review.visitPeriod == "NIGHT") R.string.review_night else R.string.review_day)
            Text(
                text = listOfNotNull(period, PlaceFormat.date(review.createdAt)).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = stringResource(
                    R.string.review_scores,
                    review.lighting,
                    review.womenPresence,
                    review.commercialVisibility,
                    review.nightExperience?.toString() ?: stringResource(R.string.detail_no_data),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            review.comment?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
internal fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.navigation_back))
    }
}
