package com.lenterosa.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lenterosa.app.account.AccountRoute
import com.lenterosa.app.account.AccountViewModel
import com.lenterosa.app.home.HomeScreen
import com.lenterosa.app.home.HomeTab
import com.lenterosa.app.map.MapViewModel
import com.lenterosa.app.places.BackButton
import com.lenterosa.app.places.ContributeScreen
import com.lenterosa.app.places.ContributeViewModel
import com.lenterosa.app.places.PlaceDetailScreen
import com.lenterosa.app.places.PlaceDetailViewModel
import com.lenterosa.app.places.PlacesViewModel
import com.lenterosa.app.privacy.PrivacyScreen

/** Pantallas de la app. El onboarding (RF-11) se agrega aquí más adelante. */
sealed interface Destination {
    /** Inicio: mapa (RF-09) y lista (RF-01). */
    data object Places : Destination
    data object Account : Destination
    data object Privacy : Destination
    data class PlaceDetail(val id: String) : Destination
    data class Contribute(val id: String, val name: String) : Destination
}

/** Pila de pantallas que sobrevive a la rotación y a que el sistema cierre la app en segundo plano. */
internal object BackStackCodec {
    private const val SEPARATOR = '\u001F'

    fun encode(stack: List<Destination>): List<String> = stack.map {
        when (it) {
            Destination.Places -> "places"
            Destination.Account -> "account"
            Destination.Privacy -> "privacy"
            is Destination.PlaceDetail -> listOf("detail", it.id).joinToString(SEPARATOR.toString())
            is Destination.Contribute -> listOf("contribute", it.id, it.name).joinToString(SEPARATOR.toString())
        }
    }

    fun decode(saved: List<String>): List<Destination> = saved.mapNotNull {
        val parts = it.split(SEPARATOR)
        when (parts.first()) {
            "places" -> Destination.Places
            "account" -> Destination.Account
            "privacy" -> Destination.Privacy
            "detail" -> parts.getOrNull(1)?.let(Destination::PlaceDetail)
            "contribute" -> if (parts.size == 3) Destination.Contribute(parts[1], parts[2]) else null
            else -> null
        }
    }.ifEmpty { listOf(Destination.Places) }
}

private val BackStackSaver = Saver<List<Destination>, List<String>>(
    save = { BackStackCodec.encode(it) },
    restore = { BackStackCodec.decode(it) },
)

// Navegación simple con una pila propia: por ahora son pocas pantallas y no justifica otra dependencia (ADR 0008).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(requestGoogleIdToken: suspend () -> String) {
    var stack by rememberSaveable(stateSaver = BackStackSaver) { mutableStateOf(listOf<Destination>(Destination.Places)) }
    var homeTab by rememberSaveable { mutableStateOf(HomeTab.MAP) }
    // Aumenta al publicar una opinión, para que la ficha se recargue con el índice actualizado.
    var detailRefresh by rememberSaveable { mutableIntStateOf(0) }
    val push: (Destination) -> Unit = { stack = stack + it }
    val pop: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) }

    BackHandler(enabled = stack.size > 1, onBack = pop)

    when (val current = stack.last()) {
        Destination.Places -> HomeScreen(
            tab = homeTab,
            onTabSelected = { homeTab = it },
            mapViewModel = hiltViewModel<MapViewModel>(),
            placesViewModel = hiltViewModel<PlacesViewModel>(),
            onOpenPlace = { push(Destination.PlaceDetail(it)) },
            onContribute = { id, name -> push(Destination.Contribute(id, name)) },
            onOpenAccount = { push(Destination.Account) },
            onOpenPrivacy = { push(Destination.Privacy) },
        )
        Destination.Privacy -> PrivacyScreen(onBack = pop)
        Destination.Account -> Scaffold(
            topBar = {
                // Sin título en la barra: la pantalla de cuenta ya muestra el suyo.
                TopAppBar(title = { }, navigationIcon = { BackButton(pop) })
            },
        ) { padding ->
            AccountRoute(
                viewModel = hiltViewModel<AccountViewModel>(),
                requestGoogleIdToken = requestGoogleIdToken,
                modifier = Modifier.padding(padding),
            )
        }
        is Destination.PlaceDetail -> PlaceDetailScreen(
            viewModel = hiltViewModel<PlaceDetailViewModel>(key = "detail-${current.id}"),
            placeId = current.id,
            refreshKey = detailRefresh,
            onBack = pop,
            onContribute = { id, name -> push(Destination.Contribute(id, name)) },
            onOpenAccount = { push(Destination.Account) },
        )
        is Destination.Contribute -> ContributeScreen(
            viewModel = hiltViewModel<ContributeViewModel>(key = "contribute-${current.id}"),
            placeId = current.id,
            placeName = current.name,
            onBack = pop,
            onPublished = {
                detailRefresh++
                pop()
            },
            onOpenAccount = { push(Destination.Account) },
        )
    }
}
