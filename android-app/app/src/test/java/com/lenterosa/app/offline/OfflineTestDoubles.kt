package com.lenterosa.app.offline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/** Caché en memoria con la misma retención de 7 días que la real. */
class FakeOfflineCache(var now: Long = 1_000_000_000_000L) : OfflineCache {
    private val json = Json { ignoreUnknownKeys = true }
    val entries = linkedMapOf<String, Pair<String, Long>>()

    override suspend fun <T> get(key: String, serializer: KSerializer<T>): Cached<T>? {
        val (text, savedAt) = entries[key] ?: return null
        if (now - savedAt > OfflineCache.RETENTION_MILLIS) {
            entries.remove(key)
            return null
        }
        return Cached(json.decodeFromString(serializer, text), savedAt)
    }

    override suspend fun <T> put(key: String, serializer: KSerializer<T>, value: T) {
        entries[key] = json.encodeToString(serializer, value) to now
    }

    override suspend fun remove(key: String) {
        entries.remove(key)
    }

    override suspend fun clearSaved() {
        entries.keys.removeAll { it.startsWith("saved:") }
    }

    override suspend fun clearAll() = entries.clear()
}

class FakeConnectivity(initial: Boolean = true) : Connectivity {
    override val online = MutableStateFlow(initial)
}
