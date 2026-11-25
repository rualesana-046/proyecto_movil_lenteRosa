package com.lenterosa.app.offline

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Caché sin conexión (RF-10, ADR 0014). Cada entrada es un JSON con la fecha en que se guardó.
 * Claves: `list:` (lugares de una zona del mapa o del centro del piloto), `place:` (ficha con sus
 * opiniones anónimas) y `saved:` (guardados y novedades de una cuenta). Los resultados de
 * "Cerca de mí" nunca se guardan: dejarían rastro de dónde estaba la usuaria.
 */
@Entity(tableName = "cache_entries")
data class CacheEntry(
    @PrimaryKey val key: String,
    val json: String,
    val savedAtMillis: Long,
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache_entries WHERE `key` = :key")
    suspend fun get(key: String): CacheEntry?

    @Upsert
    suspend fun put(entry: CacheEntry)

    @Query("DELETE FROM cache_entries WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM cache_entries WHERE savedAtMillis < :olderThanMillis")
    suspend fun deleteOlderThan(olderThanMillis: Long)

    @Query("DELETE FROM cache_entries WHERE `key` LIKE :prefix || '%'")
    suspend fun deleteByPrefix(prefix: String)

    @Query("DELETE FROM cache_entries")
    suspend fun deleteAll()
}

@Database(entities = [CacheEntry::class], version = 1, exportSchema = false)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun cache(): CacheDao
}

/** Un valor guardado y cuándo se guardó. */
data class Cached<T>(val value: T, val savedAtMillis: Long)

interface OfflineCache {
    suspend fun <T> get(key: String, serializer: KSerializer<T>): Cached<T>?
    suspend fun <T> put(key: String, serializer: KSerializer<T>, value: T)
    suspend fun remove(key: String)

    /** Borra los guardados y las novedades de cualquier cuenta (al cerrar sesión o eliminarla). */
    suspend fun clearSaved()

    /** Borra todo lo guardado para usar sin conexión. */
    suspend fun clearAll()

    companion object {
        /** La caché dura 7 días; después se borra. */
        const val RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1000

        fun listKey(lat: Double, lng: Double, radiusMeters: Int, query: String?) =
            "list:%.3f,%.3f,%d,%s".format(java.util.Locale.US, lat, lng, radiusMeters, query?.trim()?.lowercase().orEmpty())

        fun placeKey(id: String) = "place:$id"

        fun savedKey(uid: String) = "saved:$uid"
    }
}

@Singleton
class RoomOfflineCache @Inject constructor(
    private val dao: CacheDao,
    private val json: Json,
) : OfflineCache {

    /** Se puede reemplazar en pruebas. */
    var now: () -> Long = System::currentTimeMillis

    override suspend fun <T> get(key: String, serializer: KSerializer<T>): Cached<T>? {
        val entry = dao.get(key) ?: return null
        if (now() - entry.savedAtMillis > OfflineCache.RETENTION_MILLIS) {
            dao.delete(key)
            return null
        }
        return runCatching { Cached(json.decodeFromString(serializer, entry.json), entry.savedAtMillis) }.getOrNull()
    }

    // Cada escritura también borra lo que tenga más de 7 días: sin procesos programados.
    override suspend fun <T> put(key: String, serializer: KSerializer<T>, value: T) {
        val time = now()
        dao.deleteOlderThan(time - OfflineCache.RETENTION_MILLIS)
        dao.put(CacheEntry(key, json.encodeToString(serializer, value), time))
    }

    override suspend fun remove(key: String) = dao.delete(key)

    override suspend fun clearSaved() = dao.deleteByPrefix("saved:")

    override suspend fun clearAll() = dao.deleteAll()
}
