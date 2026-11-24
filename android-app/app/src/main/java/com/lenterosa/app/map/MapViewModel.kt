package com.lenterosa.app.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lenterosa.app.account.AuthGateway
import com.lenterosa.app.account.Session
import com.lenterosa.app.places.GeoPoint
import com.lenterosa.app.places.LoadState
import com.lenterosa.app.places.Loaded
import com.lenterosa.app.places.PilotArea
import com.lenterosa.app.places.PlaceSummary
import com.lenterosa.app.places.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Posición de la cámara que el mapa debe aplicar; `version` cambia en cada orden nueva. */
data class CameraTarget(val center: GeoPoint, val zoom: Double, val version: Int)

/** Avisos de "Cerca de mí", que la pantalla muestra en lenguaje cotidiano. */
enum class LocationMessage { PERMISSION_DENIED, DISABLED, UNAVAILABLE }

data class MapUiState(
    val places: LoadState<List<PlaceSummary>> = LoadState.Loading,
    val selectedId: String? = null,
    val camera: CameraTarget = CameraTarget(PilotArea.CENTER, MapViewModel.CITY_ZOOM, 0),
    /** La usuaria movió el mapa lejos de la última búsqueda: se ofrece "Buscar en esta zona". */
    val canSearchHere: Boolean = false,
    val locating: Boolean = false,
    val locationMessage: LocationMessage? = null,
) {
    val selected: PlaceSummary?
        get() = (places as? LoadState.Ready)?.value?.firstOrNull { it.id == selectedId }
}

/**
 * Mapa de lugares (RF-09). Empieza en el centro del piloto (Pasto) y solo usa la ubicación de la
 * usuaria si toca "Cerca de mí" (RNF-01). Las coordenadas se envían a la API para buscar y no se guardan.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: PlacesRepository,
    private val location: DeviceLocation,
    auth: AuthGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    val session: StateFlow<Session?> = auth.session

    private var lastSearchCenter: GeoPoint = PilotArea.CENTER
    private var visibleCenter: GeoPoint = PilotArea.CENTER
    private var visibleRadiusMeters: Int = PilotArea.RADIUS_METERS
    private var visibleZoom: Double = CITY_ZOOM

    init {
        search(PilotArea.CENTER, PilotArea.RADIUS_METERS)
    }

    /** El mapa se detuvo: se recuerda la zona visible por si la usuaria quiere buscar ahí. */
    fun onCameraIdle(center: GeoPoint, radiusMeters: Int, zoom: Double) {
        visibleCenter = center
        visibleRadiusMeters = radiusMeters.coerceIn(MIN_RADIUS_METERS, MAX_RADIUS_METERS)
        visibleZoom = zoom
        _state.update { it.copy(canSearchHere = distanceMeters(center, lastSearchCenter) > SEARCH_AGAIN_METERS) }
    }

    fun searchHere() = search(visibleCenter, visibleRadiusMeters)

    fun retry() = search(lastSearchCenter, visibleRadiusMeters)

    fun select(id: String?) = _state.update { it.copy(selectedId = id) }

    /** Una sola lectura aproximada; se busca alrededor de ella y no se guarda. */
    fun nearMe() {
        if (_state.value.locating) return
        _state.update { it.copy(locating = true, locationMessage = null) }
        viewModelScope.launch {
            when (val result = location.currentApproximate()) {
                is LocationResult.Found -> {
                    _state.update {
                        it.copy(locating = false, camera = CameraTarget(result.point, NEARBY_ZOOM, it.camera.version + 1))
                    }
                    search(result.point, NEAR_ME_RADIUS_METERS)
                }
                LocationResult.PermissionMissing -> locationFailed(LocationMessage.PERMISSION_DENIED)
                LocationResult.Disabled -> locationFailed(LocationMessage.DISABLED)
                LocationResult.Unavailable -> locationFailed(LocationMessage.UNAVAILABLE)
            }
        }
    }

    fun onLocationPermissionDenied() = locationFailed(LocationMessage.PERMISSION_DENIED)

    fun dismissLocationMessage() = _state.update { it.copy(locationMessage = null) }

    /** Al volver al mapa se conserva la zona que la usuaria estaba mirando. */
    fun currentCamera(): CameraTarget = CameraTarget(visibleCenter, visibleZoom, _state.value.camera.version)

    private fun locationFailed(message: LocationMessage) =
        _state.update { it.copy(locating = false, locationMessage = message) }

    private fun search(center: GeoPoint, radiusMeters: Int) {
        lastSearchCenter = center
        _state.update { it.copy(places = LoadState.Loading, canSearchHere = false) }
        viewModelScope.launch {
            val result = repository.nearby(center, query = null, radiusMeters = radiusMeters)
            _state.update { current ->
                val places = when (result) {
                    is Loaded.Success -> LoadState.Ready(result.value)
                    is Loaded.Failed -> LoadState.Error(result.failure)
                }
                val stillVisible = (places as? LoadState.Ready)?.value?.any { it.id == current.selectedId } == true
                current.copy(places = places, selectedId = current.selectedId.takeIf { stillVisible })
            }
        }
    }

    companion object {
        const val CITY_ZOOM = 14.0
        const val NEARBY_ZOOM = 15.0
        const val NEAR_ME_RADIUS_METERS = 1_500
        const val SEARCH_AGAIN_METERS = 300
        const val MIN_RADIUS_METERS = 300
        const val MAX_RADIUS_METERS = 5_000

        /** Distancia aproximada sobre la Tierra (fórmula del haversine), en metros. */
        fun distanceMeters(a: GeoPoint, b: GeoPoint): Int {
            val earthRadius = 6_371_000.0
            val dLat = Math.toRadians(b.lat - a.lat)
            val dLng = Math.toRadians(b.lng - a.lng)
            val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
            return (2 * earthRadius * asin(sqrt(h))).roundToInt()
        }
    }
}
