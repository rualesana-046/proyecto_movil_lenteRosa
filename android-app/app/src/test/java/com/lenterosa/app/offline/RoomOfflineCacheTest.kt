package com.lenterosa.app.offline

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Caché real en Room (RF-10): retención de 7 días y borrado por partes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class RoomOfflineCacheTest {

    private val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CacheDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private var now = 1_000_000_000_000L
    private val cache = RoomOfflineCache(database.cache(), Json).also { it.now = { now } }
    private val names = ListSerializer(String.serializer())

    @After
    fun tearDown() = database.close()

    @Test
    fun `guarda y devuelve el valor con la hora en que se guardó`() = runTest {
        cache.put("list:a", names, listOf("Parque", "Plaza"))

        assertEquals(Cached(listOf("Parque", "Plaza"), now), cache.get("list:a", names))
    }

    @Test
    fun `una entrada de más de 7 días no se devuelve y se borra`() = runTest {
        cache.put("list:a", names, listOf("Parque"))
        now += OfflineCache.RETENTION_MILLIS + 1

        assertNull(cache.get("list:a", names))
        assertNull(database.cache().get("list:a"))
    }

    @Test
    fun `al guardar algo nuevo se borran las entradas vencidas`() = runTest {
        cache.put("list:vieja", names, listOf("Parque"))
        now += OfflineCache.RETENTION_MILLIS + 1

        cache.put("list:nueva", names, listOf("Plaza"))

        assertNull(database.cache().get("list:vieja"))
    }

    @Test
    fun `una entrada de 7 días justos todavía se muestra`() = runTest {
        cache.put("list:a", names, listOf("Parque"))
        now += OfflineCache.RETENTION_MILLIS

        assertEquals(listOf("Parque"), cache.get("list:a", names)?.value)
    }

    @Test
    fun `borrar los guardados deja las listas y fichas, borrar todo no deja nada`() = runTest {
        cache.put(OfflineCache.savedKey("uid"), names, listOf("x"))
        cache.put(OfflineCache.placeKey("p1"), names, listOf("y"))

        cache.clearSaved()
        assertNull(cache.get(OfflineCache.savedKey("uid"), names))
        assertEquals(listOf("y"), cache.get(OfflineCache.placeKey("p1"), names)?.value)

        cache.clearAll()
        assertNull(cache.get(OfflineCache.placeKey("p1"), names))
    }

    @Test
    fun `la clave de una zona redondea las coordenadas a unos 100 m`() {
        assertEquals(OfflineCache.listKey(1.21361, -77.28112, 1500, null), OfflineCache.listKey(1.21364, -77.28109, 1500, null))
    }
}
