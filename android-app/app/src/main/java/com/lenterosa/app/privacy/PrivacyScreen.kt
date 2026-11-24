package com.lenterosa.app.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lenterosa.app.R
import com.lenterosa.app.map.MapConfig
import com.lenterosa.app.places.BackButton

/** Aviso de privacidad en lenguaje cotidiano: ubicación, mapa y opiniones (RNF-01, ADR 0009). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { }, navigationIcon = { BackButton(onBack) }) }) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
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
