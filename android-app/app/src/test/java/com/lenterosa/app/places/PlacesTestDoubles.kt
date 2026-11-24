package com.lenterosa.app.places

import com.lenterosa.app.account.AuthGateway
import com.lenterosa.app.account.Session
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

// Dobles de prueba para la API de lugares y la sesión.

internal fun session(verified: Boolean = true) =
    Session(uid = "uid-ana", email = "ana@ejemplo.com", emailVerified = verified, nickname = null, usesPassword = true, usesGoogle = false)

internal class FakeSessionGateway(initial: Session? = session()) : AuthGateway {
    override val session: StateFlow<Session?> = MutableStateFlow(initial)
    val tokenRequests = mutableListOf<Boolean>()

    override suspend fun idToken(forceRefresh: Boolean): String {
        tokenRequests += forceRefresh
        return "token-${tokenRequests.size}"
    }

    override suspend fun signIn(email: String, password: String) = unsupported()
    override suspend fun signUp(email: String, password: String) = unsupported()
    override suspend fun signInWithGoogle(idToken: String) = unsupported()
    override suspend fun sendEmailVerification() = unsupported()
    override suspend fun refresh() = unsupported()
    override suspend fun updateNickname(nickname: String?) = unsupported()
    override suspend fun reauthenticateWithPassword(password: String) = unsupported()
    override suspend fun reauthenticateWithGoogle(idToken: String) = unsupported()
    override suspend fun deleteFirebaseAccount() = unsupported()
    override fun signOut() = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException()
}

internal class FakePlacesApi : PlacesApi {
    var places: List<PlaceSummary> = listOf(placeSummary())
    var failure: Throwable? = null
    val nearbyQueries = mutableListOf<String?>()
    val contributions = mutableListOf<Pair<String, ReviewDraft>>()

    override suspend fun nearby(lat: Double, lng: Double, radiusMeters: Int, query: String?, page: Int, size: Int): Page<PlaceSummary> {
        nearbyQueries += query
        failure?.let { throw it }
        return Page(places, 0, size, places.size.toLong(), 1)
    }

    override suspend fun detail(id: String): PlaceDetail {
        failure?.let { throw it }
        return placeDetail(id)
    }

    override suspend fun reviews(id: String, page: Int, size: Int): Page<Review> {
        failure?.let { throw it }
        return Page(listOf(review()), 0, size, 1, 1)
    }

    override suspend fun contribute(id: String, bearer: String, review: ReviewDraft): Review {
        contributions += bearer to review
        failure?.let { throw it }
        return review()
    }
}

internal fun placeSummary(id: String = "p1") = PlaceSummary(
    id = id, name = "[FICTICIO] Parque", category = "PARK", location = Location(1.2136, -77.2811),
    distanceMeters = 120, comfortIndex = 72, comfortLabel = "COMFORTABLE",
)

internal fun placeDetail(id: String = "p1") = PlaceDetail(
    id = id, name = "[FICTICIO] Parque", category = "PARK", location = Location(1.2136, -77.2811), reviewCount = 1,
    lastReviewAt = "2026-11-01T12:00:00Z", averages = DimensionAverages(4.0, 5.0, 3.0, null),
    comfort = Comfort(index = null, label = "FEW_OPINIONS", uncertainty = "HIGH", nightDataMissing = true),
)

internal fun review() = Review("r1", 4, 5, 3, null, "DAY", null, "2026-11-01T12:00:00Z")

internal fun httpError(code: Int, body: String = "", headers: Map<String, String> = emptyMap()): HttpException {
    val response = Response.error<Any>(
        body.toResponseBody("application/problem+json".toMediaType()),
        okhttp3.Response.Builder()
            .code(code)
            .message("error")
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .headers(Headers.Builder().apply { headers.forEach { (k, v) -> add(k, v) } }.build())
            .request(okhttp3.Request.Builder().url("http://localhost/").build())
            .build(),
    )
    return HttpException(response)
}

internal fun repository(api: PlacesApi, auth: AuthGateway = FakeSessionGateway(), hasInternet: Boolean = true) =
    PlacesRepository(api, auth, { hasInternet }, Json { ignoreUnknownKeys = true })

internal fun offline() = IOException("sin red")
