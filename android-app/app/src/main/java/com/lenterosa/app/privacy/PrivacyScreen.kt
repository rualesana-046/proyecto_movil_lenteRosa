package com.lenterosa.app.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenterosa.app.R
import com.lenterosa.app.map.MapConfig
import com.lenterosa.app.offline.OfflineDataViewModel
import com.lenterosa.app.places.BackButton
import com.lenterosa.app.ui.glass.GlassTopBar
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

object PrivacyTags {
    const val CLEAR_OFFLINE = "privacy_clear_offline"
}

/** Aviso de privacidad en lenguaje cotidiano: ubicación, mapa y opiniones (RNF-01, ADR 0009). */
@Composable
fun PrivacyScreen(onBack: () -> Unit, offlineData: OfflineDataViewModel? = null) {
    val hazeState = rememberHazeState()
    Scaffold(topBar = { GlassTopBar(hazeState, navigationIcon = { BackButton(onBack) }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 16.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.privacy_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Section(R.string.privacy_location_title, stringResource(R.string.privacy_location_body))
            Section(R.string.privacy_map_title, stringResource(R.string.privacy_map_body, MapConfig.providerName))
            Section(R.string.privacy_reviews_title, stringResource(R.string.privacy_reviews_body))
            if (offlineData != null) {
                val cleared by offlineData.cleared.collectAsStateWithLifecycle()
                Section(R.string.offline_clear_title, stringResource(R.string.offline_clear_body))
                OutlinedButton(onClick = offlineData::clearAll, modifier = Modifier.fillMaxWidth().testTag(PrivacyTags.CLEAR_OFFLINE)) {
                    Text(stringResource(R.string.offline_clear_button))
                }
                if (cleared) {
                    Text(stringResource(R.string.offline_cleared), style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
        }
    }
}

@Composable
private fun Section(title: Int, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}
