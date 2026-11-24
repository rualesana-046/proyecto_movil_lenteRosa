package com.lenterosa.app.saved

import com.lenterosa.app.places.FakeSessionGateway
import com.lenterosa.app.places.LoadFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedPlacesStoreTest {

    private val api = FakeSavedApi()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `guardar y quitar un lugar se refleja al instante con el token de la sesión`() = runTest {
        val store = store(api)

        assertEquals(SaveResult.DONE, store.setSaved("p1", saved = true))
        assertEquals(setOf("p1"), store.savedIds.value)
        assertEquals(setOf("p1"), api.saved)
        assertTrue(api.bearers.all { it.startsWith("Bearer ") })

        assertEquals(SaveResult.DONE, store.setSaved("p1", saved = false))
        assertTrue(store.savedIds.value.isEmpty())
    }

    @Test
    fun `sin sesión no se guarda y se pide iniciar sesión`() = runTest {
        val store = store(api, FakeSessionGateway(initial = null))

        assertEquals(SaveResult.NOT_SIGNED_IN, store.setSaved("p1", saved = true))
        assertTrue(api.bearers.isEmpty())
        assertEquals(SavedLoad.NotSignedIn, store.overview())
    }

    @Test
    fun `si la API falla o se llega al límite, el cambio se deshace`() = runTest {
        val store = store(api)

        api.nextStatus = 409
        assertEquals(SaveResult.LIMIT_REACHED, store.setSaved("p1", saved = true))
        assertTrue(store.savedIds.value.isEmpty())

        api.nextStatus = 204
        api.offline = true
        assertEquals(SaveResult.FAILED, store.setSaved("p1", saved = true))
        assertTrue(store.savedIds.value.isEmpty())
    }

    @Test
    fun `la pestaña recibe los lugares con su ficha y los avisos, y leerlos apaga el punto`() = runTest {
        api.saved += "p1"
        api.alerts = AlertInbox(1, listOf(alert()))
        val store = store(api)

        val overview = (store.overview() as SavedLoad.Ready).overview

        assertEquals(listOf("p1"), overview.places.map { it.id })
        assertEquals(1, overview.inbox.unreadCount)
        assertEquals(1, store.unreadCount.value)

        store.markAllRead()
        assertEquals(0, store.unreadCount.value)
        assertEquals(1, api.markedRead)
    }

    @Test
    fun `sin internet la pestaña muestra un error de red y la actualización silenciosa no falla`() = runTest {
        api.offline = true

        assertEquals(SavedLoad.Failed(LoadFailure.NETWORK), store(api, hasInternet = false).overview())
        store(api).refreshQuietly()
    }
}
