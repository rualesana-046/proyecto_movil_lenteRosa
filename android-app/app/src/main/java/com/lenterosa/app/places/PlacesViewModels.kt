package com.lenterosa.app.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenterosa.app.account.AuthGateway
import com.lenterosa.app.account.Session
import com.lenterosa.app.saved.SaveResult
import com.lenterosa.app.saved.SavedPlacesStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de una carga desde la API. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Ready<T>(val value: T) : LoadState<T>
    data class Error(val failure: LoadFailure) : LoadState<Nothing>
}

private fun <T> Loaded<T>.toState(): LoadState<T> = when (this) {
    is Loaded.Success -> LoadState.Ready(value)
    is Loaded.Failed -> LoadState.Error(failure)
}

data class PlacesUiState(val query: String = "", val places: LoadState<List<PlaceSummary>> = LoadState.Loading)

/** Lista de lugares alrededor del centro del piloto, con búsqueda por nombre (RF-01). */
@HiltViewModel
class PlacesViewModel @Inject constructor(private val repository: PlacesRepository) : ViewModel() {

    private val _state = MutableStateFlow(PlacesUiState())
    val state: StateFlow<PlacesUiState> = _state.asStateFlow()

    init {
        search()
    }

    fun onQueryChange(query: String) = _state.update { it.copy(query = query) }

    fun search() {
        val query = _state.value.query
        _state.update { it.copy(places = LoadState.Loading) }
        viewModelScope.launch {
            val result = repository.nearby(PilotArea.CENTER, query).toState()
            _state.update { it.copy(places = result) }
        }
    }
}

/** Ficha de un lugar (RF-01 y RF-03) y si la usuaria puede opinar (RF-04). */
@HiltViewModel
class PlaceDetailViewModel @Inject constructor(
    private val repository: PlacesRepository,
    auth: AuthGateway,
    private val saved: SavedPlacesStore,
) : ViewModel() {

    val savedIds: StateFlow<Set<String>> = saved.savedIds

    private val _saveMessage = MutableStateFlow<SaveResult?>(null)
    val saveMessage: StateFlow<SaveResult?> = _saveMessage.asStateFlow()

    fun toggleSaved(placeId: String) {
        viewModelScope.launch {
            val result = saved.setSaved(placeId, saved = placeId !in saved.savedIds.value)
            if (result != SaveResult.DONE) _saveMessage.value = result
        }
    }

    fun dismissSaveMessage() {
        _saveMessage.value = null
    }


    private val _place = MutableStateFlow<LoadState<PlaceWithReviews>>(LoadState.Loading)
    val place: StateFlow<LoadState<PlaceWithReviews>> = _place.asStateFlow()

    val session: StateFlow<Session?> = auth.session

    private var placeId: String? = null

    /** Carga el lugar la primera vez; con `force` vuelve a cargarlo (por ejemplo, después de opinar). */
    fun load(id: String, force: Boolean = false) {
        if (id == placeId && !force && _place.value !is LoadState.Error) {
            return
        }
        placeId = id
        _place.value = LoadState.Loading
        viewModelScope.launch {
            _place.value = repository.placeWithReviews(id).toState()
        }
    }

    fun retry() {
        placeId?.let { load(it, force = true) }
    }
}
