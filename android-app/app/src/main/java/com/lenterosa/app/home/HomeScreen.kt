package com.lenterosa.app.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenterosa.app.R
import com.lenterosa.app.discreet.DiscreetActivity
import com.lenterosa.app.map.MapAttribution
import com.lenterosa.app.map.MapTab
import com.lenterosa.app.map.MapViewModel
import com.lenterosa.app.map.PlacesMap
import com.lenterosa.app.places.LoadState
import com.lenterosa.app.places.PlaceSummary
import com.lenterosa.app.places.PlacesContent
import com.lenterosa.app.places.PlacesViewModel
import com.lenterosa.app.saved.SavedContent
import com.lenterosa.app.saved.SavedViewModel
import com.lenterosa.app.saved.saveResultMessage
import com.lenterosa.app.ui.glass.GlassNavigationBar
import com.lenterosa.app.ui.glass.GlassTopBar
import com.lenterosa.app.ui.glass.currentMotion
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

enum class HomeTab { MAP, LIST, SAVED }

object HomeTags {
    const val BOTTOM_BAR = "home_bottom_bar"
    const val ALERTS_BADGE = "home_alerts_badge"
    const val TITLE = "home_title"
}

/**
 * Estructura de inicio: barra superior de vidrio, contenido y, abajo, la atribución del mapa (solo
 * en la pestaña del mapa) seguida de la barra de navegación de vidrio. El contenido pasa por detrás
 * de las barras de vidrio, pero la atribución es opaca y queda fuera de esa zona: ni el vidrio ni
 * las tarjetas ni los botones del mapa pueden taparla (ADR 0009 y 0010).
 */
@Composable
fun HomeScaffold(
    tab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAccount: () -> Unit,
    unreadAlerts: Int = 0,
    onOpenDiscreet: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (padding: PaddingValues, tab: HomeTab, backdrop: Modifier) -> Unit,
) {
    val hazeState = rememberHazeState()
    val motion = currentMotion()
    // La lista se desenfoca detrás de las barras. El mapa no: su vista nativa no se puede capturar,
    // así que sobre él las barras usan el tinte sin desenfoque.
    val barBackdrop = hazeState.takeIf { tab != HomeTab.MAP }
    Scaffold(
        topBar = {
            GlassTopBar(
                hazeState = barBackdrop,
                // Gesto para el Modo discreto: mantener presionado el título. Tocarlo no hace nada, así que
                // no choca con la navegación; TalkBack lo ofrece como acción con nombre.
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        modifier = Modifier
                            .testTag(HomeTags.TITLE)
                            .combinedClickable(
                                interactionSource = null,
                                indication = null,
                                onClick = {},
                                onLongClickLabel = stringResource(R.string.discreet_open),
                                onLongClick = onOpenDiscreet,
                            ),
                    )
                },
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
                GlassNavigationBar(barBackdrop, Modifier.testTag(HomeTags.BOTTOM_BAR)) {
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
                    // Aviso sutil: solo un punto en la pestaña, sin sonido ni vibración. El lector de
                    // pantalla lo anuncia con texto.
                    val alertsDescription = pluralStringResource(R.plurals.saved_unread_alerts, unreadAlerts, unreadAlerts)
                    NavigationBarItem(
                        selected = tab == HomeTab.SAVED,
                        onClick = { onTabSelected(HomeTab.SAVED) },
                        icon = {
                            BadgedBox(badge = { if (unreadAlerts > 0) Badge(Modifier.testTag(HomeTags.ALERTS_BADGE)) }) {
                                Icon(painterResource(R.drawable.ic_bookmark), contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(R.string.home_tab_saved)) },
                        modifier = if (unreadAlerts > 0) Modifier.semantics { stateDescription = alertsDescription } else Modifier,
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = motion.tabTransition(),
            modifier = Modifier.fillMaxSize(),
            label = "home-tab",
        ) { shownTab ->
            content(padding, shownTab, if (shownTab != HomeTab.MAP) Modifier.hazeSource(hazeState) else Modifier)
        }
    }
}

/** Inicio con el mapa (RF-09), la lista (RF-01) y los guardados con sus avisos (RF-05, RF-07). */
@Composable
fun HomeScreen(
    tab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    mapViewModel: MapViewModel,
    placesViewModel: PlacesViewModel,
    savedViewModel: SavedViewModel,
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
    val savedIds by mapViewModel.savedIds.collectAsStateWithLifecycle()
    val unreadAlerts by mapViewModel.unreadAlerts.collectAsStateWithLifecycle()
    val saveMessage by mapViewModel.saveMessage.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    // Al entrar o al cambiar de sesión se actualizan los guardados y los avisos sin leer.
    LaunchedEffect(session?.uid) { mapViewModel.refreshSaved() }
    val messageText = saveMessage?.let { saveResultMessage(it) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            mapViewModel.dismissSaveMessage()
        }
    }

    HomeScaffold(
        tab,
        onTabSelected,
        onOpenPrivacy,
        onOpenAccount,
        unreadAlerts,
        onOpenDiscreet = { context.startActivity(DiscreetActivity.intent(context)) },
        snackbarHostState = snackbar,
    ) { padding, shownTab, backdrop ->
        when (shownTab) {
            HomeTab.MAP -> MapTab(
                contentPadding = padding,
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
                savedIds = savedIds,
                onToggleSaved = { place -> mapViewModel.toggleSaved(place.id) },
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
            )
            HomeTab.LIST -> PlacesContent(placesViewModel, onOpenPlace, modifier = backdrop, contentPadding = padding)
            HomeTab.SAVED -> SavedContent(savedViewModel, onOpenPlace, onOpenAccount, modifier = backdrop, contentPadding = padding)
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
