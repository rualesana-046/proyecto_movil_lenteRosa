package com.lenterosa.app.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Pestaña Guardados: avisos y lugares guardados (RF-05, RF-07). */
@HiltViewModel
class SavedViewModel @Inject constructor(private val store: SavedPlacesStore) : ViewModel() {

    private val _state = MutableStateFlow<SavedLoad?>(null)

    /** Nulo mientras carga. */
    val state: StateFlow<SavedLoad?> = _state.asStateFlow()

    fun load() {
        _state.value = null
        viewModelScope.launch {
            val result = store.overview()
            _state.value = result
            // Al mostrarlos, los avisos quedan leídos; en pantalla siguen marcados como "Nuevo" esta vez.
            if (result is SavedLoad.Ready && result.overview.inbox.unreadCount > 0) {
                store.markAllRead()
            }
        }
    }

    fun remove(placeId: String) {
        viewModelScope.launch {
            if (store.setSaved(placeId, saved = false) == SaveResult.DONE) {
                val current = _state.value
                if (current is SavedLoad.Ready) {
                    _state.value = SavedLoad.Ready(
                        current.overview.copy(places = current.overview.places.filterNot { it.id == placeId }),
                    )
                }
            }
        }
    }
}
