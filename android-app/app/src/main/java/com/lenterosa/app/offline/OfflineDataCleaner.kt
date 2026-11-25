package com.lenterosa.app.offline

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenterosa.app.map.MapConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.maplibre.android.offline.OfflineManager

/** Borra las teselas del mapa guardadas en el teléfono. */
fun interface MapCacheCleaner {
    suspend fun clear(): Boolean
}

class MapLibreCacheCleaner @Inject constructor(@ApplicationContext private val context: Context) : MapCacheCleaner {
    override suspend fun clear(): Boolean = withContext(Dispatchers.Main) {
        MapConfig.initialize(context)
        suspendCancellableCoroutine { continuation ->
            OfflineManager.getInstance(context).clearAmbientCache(object : OfflineManager.FileSourceCallback {
                override fun onSuccess() = continuation.resume(true)
                override fun onError(message: String) = continuation.resume(false)
            })
        }
    }
}

/** "Borrar datos sin conexión" en Tu privacidad (RF-10): lugares, fichas, guardados y mapa. */
@HiltViewModel
class OfflineDataViewModel @Inject constructor(
    private val cache: OfflineCache,
    private val mapCache: MapCacheCleaner,
) : ViewModel() {

    private val _cleared = MutableStateFlow(false)
    val cleared: StateFlow<Boolean> = _cleared.asStateFlow()

    fun clearAll() {
        viewModelScope.launch {
            cache.clearAll()
            mapCache.clear()
            _cleared.value = true
        }
    }

    fun dismiss() {
        _cleared.value = false
    }
}
