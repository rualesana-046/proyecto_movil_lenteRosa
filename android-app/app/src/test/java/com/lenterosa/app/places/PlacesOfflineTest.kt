package com.lenterosa.app.places

import com.lenterosa.app.offline.FakeOfflineCache
import com.lenterosa.app.offline.OfflineCache
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-10: sin conexión se muestra lo último guardado, con su fecha y el motivo. */
class PlacesOfflineTest {

    private val api = FakePlacesApi()
    private val cache = FakeOfflineCache()
    private val hour = 60L * 60 * 1000

    @Test
    fun `sin conexión la lista vuelve de la caché con la hora en que se guardó`() = runTest {
        val savedAt = cache.now
        repository(api, cache = cache).nearby(PilotArea.CENTER, null)
        cache.now += 3 * hour
        api.failure = offline()

        val result = repository(api, hasInternet = false, cache = cache).nearby(PilotArea.CENTER, null) as Loaded.Success

        assertEquals(listOf(placeSummary()), result.value)
        assertEquals(savedAt, result.cachedAtMillis)
        assertEquals(LoadFailure.NETWORK, result.staleBecause)
    }

    @Test
    fun `una respuesta nueva no se marca como guardada`() = runTest {
        val result = repository(api, cache = cache).nearby(PilotArea.CENTER, null) as Loaded.Success

        assertNull(result.cachedAtMillis)
        assertNull(result.staleBecause)
    }

    @Test
    fun `si el servidor falla también se usa lo guardado, con ese motivo`() = runTest {
        repository(api, cache = cache).placeWithReviews("p1")
        api.failure = httpError(500)

        val result = repository(api, cache = cache).placeWithReviews("p1") as Loaded.Success

        assertEquals("p1", result.value.place.id)
        assertEquals(1, result.value.reviews.size)
        assertEquals(LoadFailure.SERVER, result.staleBecause)
    }

    @Test
    fun `los resultados de Cerca de mí nunca se guardan en el teléfono`() = runTest {
        repository(api, cache = cache).nearby(GeoPoint(1.20, -77.27), null, radiusMeters = 800, cacheable = false)

        assertTrue(cache.entries.isEmpty())
    }

    @Test
    fun `Cerca de mí sin conexión no muestra resultados guardados de otra búsqueda`() = runTest {
        val point = GeoPoint(1.20, -77.27)
        repository(api, cache = cache).nearby(point, null, radiusMeters = 800)
        api.failure = offline()

        val result = repository(api, hasInternet = false, cache = cache).nearby(point, null, radiusMeters = 800, cacheable = false)

        assertEquals(Loaded.Failed(LoadFailure.NETWORK), result)
    }

    @Test
    fun `un lugar que ya no existe se borra de la caché`() = runTest {
        repository(api, cache = cache).placeWithReviews("p1")
        api.failure = httpError(404)

        assertEquals(Loaded.Failed(LoadFailure.NOT_FOUND), repository(api, cache = cache).placeWithReviews("p1"))
        assertTrue(OfflineCache.placeKey("p1") !in cache.entries)
    }

    @Test
    fun `lo guardado hace más de 7 días ya no se muestra`() = runTest {
        repository(api, cache = cache).nearby(PilotArea.CENTER, null)
        cache.now += OfflineCache.RETENTION_MILLIS + 1
        api.failure = offline()

        assertEquals(Loaded.Failed(LoadFailure.NETWORK), repository(api, hasInternet = false, cache = cache).nearby(PilotArea.CENTER, null))
    }

    @Test
    fun `cada zona y búsqueda del mapa se guarda por separado`() = runTest {
        repository(api, cache = cache).nearby(PilotArea.CENTER, "parque")
        repository(api, cache = cache).nearby(GeoPoint(1.22, -77.29), null)

        assertEquals(2, cache.entries.size)
    }

    @Test
    fun `el estado de la pantalla lleva la fecha y el motivo de los datos guardados`() {
        val state = Loaded.Success(listOf(placeSummary()), cachedAtMillis = 42L, staleBecause = LoadFailure.NETWORK).toState()

        assertEquals(Staleness(42L, LoadFailure.NETWORK), (state as LoadState.Ready).stale)
        assertNull((Loaded.Success(1).toState() as LoadState.Ready).stale)
    }
}
