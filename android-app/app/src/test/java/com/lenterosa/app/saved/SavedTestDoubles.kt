package com.lenterosa.app.saved

import com.lenterosa.app.account.AuthGateway
import com.lenterosa.app.places.FakePlacesApi
import com.lenterosa.app.places.FakeSessionGateway
import java.io.IOException
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

// Doble de la API de guardados y avisos.
internal class FakeSavedApi : SavedApi {
    val saved = linkedSetOf<String>()
    var alerts = AlertInbox(0, emptyList())
    var nextStatus = 204
    var offline = false
    val bearers = mutableListOf<String>()
    var markedRead = 0

    private fun check(bearer: String) {
        bearers += bearer
        if (offline) throw IOException("sin red")
    }

    override suspend fun savedPlaces(bearer: String): List<SavedPlaceDto> {
        check(bearer)
        return saved.map { SavedPlaceDto(it, "2026-11-20T12:00:00Z") }
    }

    override suspend fun save(bearer: String, placeId: String): Response<Unit> {
        check(bearer)
        return respond { saved += placeId }
    }

    override suspend fun remove(bearer: String, placeId: String): Response<Unit> {
        check(bearer)
        return respond { saved -= placeId }
    }

    override suspend fun alerts(bearer: String): AlertInbox {
        check(bearer)
        return alerts
    }

    override suspend fun markAllRead(bearer: String): Response<Unit> {
        check(bearer)
        markedRead++
        alerts = alerts.copy(unreadCount = 0, items = alerts.items.map { it.copy(read = true) })
        return Response.success(204, Unit)
    }

    private fun respond(onSuccess: () -> Unit): Response<Unit> =
        if (nextStatus in 200..299) {
            onSuccess()
            Response.success(nextStatus, Unit)
        } else {
            Response.error(nextStatus, "{}".toResponseBody())
        }
}

internal fun alert(placeId: String = "p1", read: Boolean = false) =
    AlertDto("a1", placeId, "COMFORTABLE", 66, "MIXED_OPINIONS", 52, "2026-11-20T12:00:00Z", read)

internal fun store(
    api: SavedApi = FakeSavedApi(),
    auth: AuthGateway = FakeSessionGateway(),
    hasInternet: Boolean = true,
    cache: com.lenterosa.app.offline.OfflineCache = com.lenterosa.app.offline.FakeOfflineCache(),
) = SavedPlacesStore(api, FakePlacesApi(), auth, { hasInternet }, cache)
