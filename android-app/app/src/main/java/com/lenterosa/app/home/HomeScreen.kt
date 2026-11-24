package com.lenterosa.app.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenterosa.app.R
import com.lenterosa.app.map.MapAttribution
import com.lenterosa.app.map.MapTab
import com.lenterosa.app.map.MapViewModel
import com.lenterosa.app.map.PlacesMap
import com.lenterosa.app.places.LoadState
import com.lenterosa.app.places.PlaceSummary
import com.lenterosa.app.places.PlacesContent
import com.lenterosa.app.places.PlacesViewModel

enum class HomeTab { MAP, LIST }

object HomeTags {
    const val BOTTOM_BAR = "home_bottom_bar"
}

/**
 * Estructura de inicio: barra superior, contenido y, abajo, la atribución del mapa (solo en la
 * pestaña del mapa) seguida de la barra de navegación. La atribución queda fuera de la zona del
 * contenido, así que ni las tarjetas ni los botones del mapa pueden taparla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScaffold(
    tab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAccount: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenPrivacy) {
                        Icon(painterResource(R.drawable.ic_privacy), contentDescription = stringResource(R.string.privacy_title))
                    }
                    IconButton(onClick = onOpenAccount) {
                        Icon(painterResource(R.drawable.ic_account), contentDescription = stringResource(R.string.places_open_account))
                    }
                },
            )
        },
        bottomBar = {
            Column {
                if (tab == HomeTab.MAP) {
                    MapAttribution()
                }
                NavigationBar(Modifier.testTag(HomeTags.BOTTOM_BAR)) {
                    NavigationBarItem(
                        selected = tab == HomeTab.MAP,
                        onClick = { onTabSelected(HomeTab.MAP) },
                        icon = { Icon(painterResource(R.drawable.ic_map), contentDescription = null) },
                        label = { Text(stringResource(R.string.home_tab_map)) },
                    )
                    NavigationBarItem(
                        selected = tab == HomeTab.LIST,
                        onClick = { onTabSelected(HomeTab.LIST) },
                        icon = { Icon(painterResource(R.drawable.ic_list), contentDescription = null) },
                        label = { Text(stringResource(R.string.home_tab_list)) },
                    )
                }
            }
        },
        content = content,
    )
}

/** Inicio con el mapa (RF-09) y la lista (RF-01). */
@Composable
fun HomeScreen(
    tab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    mapViewModel: MapViewModel,
    placesViewModel: PlacesViewModel,
    onOpenPlace: (String) -> Unit,
    onContribute: (id: String, name: String) -> Unit,
    onOpenAccount: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val state by mapViewModel.state.collectAsStateWithLifecycle()
    val session by mapViewModel.session.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var explainLocation by rememberSaveable { mutableStateOf(false) }
    val requestLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) mapViewModel.nearMe() else mapViewModel.onLocationPermissionDenied()
    }
    // Se lee una vez al entrar: así, al volver de la ficha, el mapa sigue en la zona que se miraba.
    val initialCamera = remember { mapViewModel.currentCamera() }

    HomeScaffold(tab, onTabSelected, onOpenPrivacy, onOpenAccount) { padding ->
        when (tab) {
            HomeTab.MAP -> MapTab(
                state = state,
                onNearMe = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) mapViewModel.nearMe() else explainLocation = true
                },
                onSearchHere = mapViewModel::searchHere,
                onRetry = mapViewModel::retry,
                onDismissLocationMessage = mapViewModel::dismissLocationMessage,
                onOpenPlace = onOpenPlace,
                onContribute = { place: PlaceSummary ->
                    if (session?.emailVerified == true) onContribute(place.id, place.name) else onOpenPlace(place.id)
                },
                onCloseCard = { mapViewModel.select(null) },
                map = { modifier ->
                    PlacesMap(
                        places = (state.places as? LoadState.Ready)?.value.orEmpty(),
                        selectedId = state.selectedId,
                        camera = state.camera,
                        initialCamera = initialCamera,
                        onSelect = mapViewModel::select,
                        onCameraIdle = mapViewModel::onCameraIdle,
                        modifier = modifier,
                    )
                },
                modifier = Modifier.padding(padding),
            )
            HomeTab.LIST -> PlacesContent(placesViewModel, onOpenPlace, Modifier.padding(padding))
        }
    }

    if (explainLocation) {
        AlertDialog(
            onDismissRequest = { explainLocation = false },
            title = { Text(stringResource(R.string.map_location_title)) },
            text = { Text(stringResource(R.string.map_location_explanation)) },
            confirmButton = {
                TextButton(onClick = {
                    explainLocation = false
                    requestLocation.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }) { Text(stringResource(R.string.map_location_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { explainLocation = false }) { Text(stringResource(R.string.account_cancel)) }
            },
        )
    }
}
