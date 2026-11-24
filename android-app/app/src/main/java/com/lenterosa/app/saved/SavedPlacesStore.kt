package com.lenterosa.app.saved

import android.util.Log
import com.lenterosa.app.BuildConfig
import com.lenterosa.app.account.AuthException
import com.lenterosa.app.account.AuthGateway
import com.lenterosa.app.network.NetworkStatus
import com.lenterosa.app.places.LoadFailure
import com.lenterosa.app.places.PlaceDetail
import com.lenterosa.app.places.PlacesApi
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException

/** Resultado de guardar o quitar un lugar. */
enum class SaveResult { DONE, NOT_SIGNED_IN, LIMIT_REACHED, FAILED }

/** Lo que muestra la pestaña Guardados. */
data class SavedOverview(val places: List<PlaceDetail>, val inbox: AlertInbox)

sealed interface SavedLoad {
    data object NotSignedIn : SavedLoad
    data class Ready(val overview: SavedOverview) : SavedLoad
    data class Failed(val failure: LoadFailure) : SavedLoad
}

/**
 * Lugares guardados y avisos de la usuaria, compartidos por la ficha, la tarjeta del mapa y la
 * pestaña Guardados. Sin sesión no hay guardados: guardar lleva a iniciar sesión.
 */
@Singleton
class SavedPlacesStore @Inject constructor(
    private val api: SavedApi,
    private val places: PlacesApi,
    private val auth: AuthGateway,
    private val network: NetworkStatus,
) {

    private val _savedIds = MutableStateFlow<Set<String>>(emptySet())
    val savedIds: StateFlow<Set<String>> = _savedIds.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)

    /** Avisos sin leer: la pestaña Guardados muestra un punto discreto si hay alguno. */
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    /** Vuelve a leer los guardados y los avisos sin leer, sin mostrar errores (se llama al abrir el inicio). */
    suspend fun refreshQuietly() {
        if (auth.session.value == null) {
            _savedIds.value = emptySet()
            _unreadCount.value = 0
            return
        }
        try {
            val bearer = bearer()
            _savedIds.value = api.savedPlaces(bearer).map { it.placeId }.toSet()
            _unreadCount.value = api.alerts(bearer).unreadCount
        } catch (e: Exception) {
            if (e is IOException || e is HttpException || e is AuthException) {
                if (BuildConfig.DEBUG) Log.w(TAG, "No se pudieron actualizar los guardados: ${e.javaClass.simpleName}")
            } else {
                throw e
            }
        }
    }

    /** Guardados con su ficha y avisos, para la pestaña Guardados. */
    suspend fun overview(): SavedLoad {
        if (auth.session.value == null) return SavedLoad.NotSignedIn
        return try {
            val bearer = bearer()
            val saved = api.savedPlaces(bearer)
            val inbox = api.alerts(bearer)
            val details = coroutineScope { saved.map { async { runCatching { places.detail(it.placeId) }.getOrNull() } }.awaitAll() }
            _savedIds.value = saved.map { it.placeId }.toSet()
            _unreadCount.value = inbox.unreadCount
            SavedLoad.Ready(SavedOverview(details.filterNotNull(), inbox))
        } catch (e: HttpException) {
            if (e.code() == 401) SavedLoad.NotSignedIn else SavedLoad.Failed(LoadFailure.SERVER)
        } catch (e: IOException) {
            SavedLoad.Failed(if (network.hasInternet()) LoadFailure.SERVER_UNREACHABLE else LoadFailure.NETWORK)
        } catch (e: AuthException) {
            SavedLoad.NotSignedIn
        }
    }

    /** Guarda o quita el lugar. El cambio se ve al instante y se deshace si la API falla. */
    suspend fun setSaved(placeId: String, saved: Boolean): SaveResult {
        if (auth.session.value == null) return SaveResult.NOT_SIGNED_IN
        val previous = _savedIds.value
        _savedIds.update { if (saved) it + placeId else it - placeId }
        return try {
            val bearer = bearer()
            val response = if (saved) api.save(bearer, placeId) else api.remove(bearer, placeId)
            when {
                response.isSuccessful -> SaveResult.DONE
                response.code() == 409 -> SaveResult.LIMIT_REACHED.also { _savedIds.value = previous }
                response.code() == 401 -> SaveResult.NOT_SIGNED_IN.also { _savedIds.value = previous }
                else -> SaveResult.FAILED.also { _savedIds.value = previous }
            }
        } catch (e: Exception) {
            if (e is IOException || e is HttpException || e is AuthException) {
                _savedIds.value = previous
                SaveResult.FAILED
            } else {
                throw e
            }
        }
    }

    /** Marca los avisos como leídos al verlos; si falla, se intentará la próxima vez. */
    suspend fun markAllRead() {
        if (auth.session.value == null || _unreadCount.value == 0) return
        try {
            if (api.markAllRead(bearer()).isSuccessful) _unreadCount.value = 0
        } catch (e: Exception) {
            if (e !is IOException && e !is HttpException && e !is AuthException) throw e
        }
    }

    private suspend fun bearer() = "Bearer ${auth.idToken()}"
}

private const val TAG = "SavedPlacesStore"
