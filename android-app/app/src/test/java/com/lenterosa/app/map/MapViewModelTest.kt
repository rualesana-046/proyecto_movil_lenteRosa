package com.lenterosa.app.map

import com.lenterosa.app.places.FakePlacesApi
import com.lenterosa.app.places.FakeSessionGateway
import com.lenterosa.app.places.GeoPoint
import com.lenterosa.app.places.LoadState
import com.lenterosa.app.places.PilotArea
import com.lenterosa.app.places.PlacesApi
import com.lenterosa.app.places.Page
import com.lenterosa.app.places.PlaceSummary
import com.lenterosa.app.places.placeSummary
import com.lenterosa.app.places.repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val api = RecordingPlacesApi()
    private var locationResult: LocationResult = LocationResult.Found(GeoPoint(1.20, -77.27))
    private var locationRequests = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MapViewModel(
        repository(api),
        { locationRequests++; locationResult },
        FakeSessionGateway(),
        com.lenterosa.app.saved.store(),
        com.lenterosa.app.offline.FakeConnectivity(),
    )

    @Test
    fun `el mapa empieza en Pasto sin pedir la ubicación`() {
        val viewModel = viewModel()

        assertEquals(listOf(Search(PilotArea.CENTER, PilotArea.RADIUS_METERS)), api.searches)
        assertEquals(PilotArea.CENTER, viewModel.state.value.camera.center)
        assertEquals(0, locationRequests)
        assertTrue(viewModel.state.value.places is LoadState.Ready)
    }

    @Test
    fun `Cerca de mí mueve el mapa y busca alrededor de la ubicación, una sola vez`() {
        val viewModel = viewModel()

        viewModel.nearMe()

        val camera = viewModel.state.value.camera
        assertEquals(GeoPoint(1.20, -77.27), camera.center)
        assertEquals(1, camera.version)
        assertEquals(Search(GeoPoint(1.20, -77.27), MapViewModel.NEAR_ME_RADIUS_METERS), api.searches.last())
        assertEquals(1, locationRequests)
        assertFalse(viewModel.state.value.locating)
    }

    @Test
    fun `sin permiso o con la ubicación apagada se avisa y el mapa sigue en Pasto`() {
        locationResult = LocationResult.PermissionMissing
        val viewModel = viewModel()

        viewModel.nearMe()

        assertEquals(LocationMessage.PERMISSION_DENIED, viewModel.state.value.locationMessage)
        assertEquals(PilotArea.CENTER, viewModel.state.value.camera.center)

        locationResult = LocationResult.Disabled
        viewModel.nearMe()

        assertEquals(LocationMessage.DISABLED, viewModel.state.value.locationMessage)
        assertEquals(1, api.searches.size)
    }

    @Test
    fun `al mover el mapa lejos se ofrece buscar en esa zona, con un radio de 5 km como máximo`() {
        val viewModel = viewModel()
        val elsewhere = GeoPoint(1.2300, -77.2811)

        viewModel.onCameraIdle(PilotArea.CENTER, 900, 14.0)
        assertFalse(viewModel.state.value.canSearchHere)

        viewModel.onCameraIdle(elsewhere, 20_000, 12.0)
        assertTrue(viewModel.state.value.canSearchHere)

        viewModel.searchHere()

        assertEquals(Search(elsewhere, MapViewModel.MAX_RADIUS_METERS), api.searches.last())
        assertFalse(viewModel.state.value.canSearchHere)
        assertEquals(elsewhere, viewModel.currentCamera().center)
    }

    @Test
    fun `el lugar elegido se deselecciona si ya no aparece en la nueva búsqueda`() {
        val viewModel = viewModel()
        viewModel.select("p1")
        assertEquals("p1", viewModel.state.value.selected?.id)

        api.places = listOf(placeSummary("p2"))
        viewModel.onCameraIdle(GeoPoint(1.25, -77.28), 1_000, 14.0)
        viewModel.searchHere()

        assertNull(viewModel.state.value.selectedId)
    }

    @Test
    fun `la distancia entre dos puntos de Pasto es aproximada en metros`() {
        val meters = MapViewModel.distanceMeters(GeoPoint(1.2136, -77.2811), GeoPoint(1.2200, -77.2790))
        assertTrue("$meters", meters in 700..790)
    }

    private data class Search(val center: GeoPoint, val radius: Int)

    private class RecordingPlacesApi(private val delegate: FakePlacesApi = FakePlacesApi()) : PlacesApi by delegate {
        val searches = mutableListOf<Search>()
        var places: List<PlaceSummary>
            get() = delegate.places
            set(value) {
                delegate.places = value
            }

        override suspend fun nearby(lat: Double, lng: Double, radiusMeters: Int, query: String?, page: Int, size: Int): Page<PlaceSummary> {
            searches += Search(GeoPoint(lat, lng), radiusMeters)
            return delegate.nearby(lat, lng, radiusMeters, query, page, size)
        }
    }
}
