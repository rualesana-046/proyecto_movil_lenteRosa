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
import com.lenterosa.app.places.BackButton
import com.lenterosa.app.places.ContributeScreen
import com.lenterosa.app.places.ContributeViewModel
import com.lenterosa.app.places.PlaceDetailScreen
import com.lenterosa.app.places.PlaceDetailViewModel
import com.lenterosa.app.places.PlacesScreen
import com.lenterosa.app.places.PlacesViewModel

/** Pantallas de la app. El mapa (RF-09) y el onboarding (RF-11) se agregan aquí más adelante. */
sealed interface Destination {
    data object Places : Destination
    data object Account : Destination
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
            is Destination.PlaceDetail -> listOf("detail", it.id).joinToString(SEPARATOR.toString())
            is Destination.Contribute -> listOf("contribute", it.id, it.name).joinToString(SEPARATOR.toString())
        }
    }

    fun decode(saved: List<String>): List<Destination> = saved.mapNotNull {
        val parts = it.split(SEPARATOR)
        when (parts.first()) {
            "places" -> Destination.Places
            "account" -> Destination.Account
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
    // Aumenta al publicar una opinión, para que la ficha se recargue con el índice actualizado.
    var detailRefresh by rememberSaveable { mutableIntStateOf(0) }
    val push: (Destination) -> Unit = { stack = stack + it }
    val pop: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) }

    BackHandler(enabled = stack.size > 1, onBack = pop)

    when (val current = stack.last()) {
        Destination.Places -> PlacesScreen(
            viewModel = hiltViewModel<PlacesViewModel>(),
            onOpenPlace = { push(Destination.PlaceDetail(it)) },
            onOpenAccount = { push(Destination.Account) },
        )
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
