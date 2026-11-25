package com.lenterosa.app.saved

import com.lenterosa.app.offline.FakeOfflineCache
import com.lenterosa.app.offline.OfflineCache
import com.lenterosa.app.places.FakeSessionGateway
import com.lenterosa.app.places.LoadFailure
import com.lenterosa.app.places.session
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-10: los guardados y avisos se ven sin conexión y solo para la cuenta que los guardó. */
class SavedOfflineTest {

    private val api = FakeSavedApi().apply { saved += "p1"; alerts = AlertInbox(1, listOf(alert())) }
    private val cache = FakeOfflineCache()

    @Test
    fun `sin conexión se ven los guardados y avisos de la última vez, con su fecha`() = runTest {
        store(api, cache = cache).overview()
        api.offline = true

        val result = store(api, hasInternet = false, cache = cache).overview() as SavedLoad.Ready

        assertEquals(listOf("p1"), result.overview.places.map { it.id })
        assertEquals(1, result.overview.inbox.items.size)
        assertEquals(LoadFailure.NETWORK, result.stale?.reason)
        assertEquals(cache.now, result.stale?.savedAtMillis)
    }

    @Test
    fun `sin conexión y sin nada guardado se muestra el error de red`() = runTest {
        api.offline = true

        assertEquals(SavedLoad.Failed(LoadFailure.NETWORK), store(api, hasInternet = false, cache = cache).overview())
    }

    @Test
    fun `lo guardado de una cuenta no se muestra a otra`() = runTest {
        cache.put(OfflineCache.savedKey("otra-cuenta"), SavedOverview.serializer(), SavedOverview(emptyList(), AlertInbox(0, emptyList())))
        api.offline = true

        val result = store(api, FakeSessionGateway(session()), hasInternet = false, cache = cache).overview()

        assertEquals(SavedLoad.Failed(LoadFailure.NETWORK), result)
    }

    @Test
    fun `sin sesión se borran los guardados del teléfono pero no las fichas de lugares`() = runTest {
        cache.put(OfflineCache.savedKey("uid-ana"), String.serializer(), "guardados")
        cache.put(OfflineCache.placeKey("p1"), String.serializer(), "ficha")

        val result = store(api, FakeSessionGateway(initial = null), cache = cache).overview()

        assertEquals(SavedLoad.NotSignedIn, result)
        assertNull(cache.get(OfflineCache.savedKey("uid-ana"), String.serializer()))
        assertTrue(OfflineCache.placeKey("p1") in cache.entries)
    }

    @Test
    fun `al abrir el inicio sin sesión también se borran los guardados`() = runTest {
        cache.put(OfflineCache.savedKey("uid-ana"), String.serializer(), "guardados")

        store(api, FakeSessionGateway(initial = null), cache = cache).refreshQuietly()

        assertTrue(cache.entries.isEmpty())
    }
}
