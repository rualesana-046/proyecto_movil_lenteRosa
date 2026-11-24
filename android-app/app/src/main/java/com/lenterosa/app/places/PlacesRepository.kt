package com.lenterosa.app.places

import android.util.Log
import com.lenterosa.app.BuildConfig
import com.lenterosa.app.account.AuthException
import com.lenterosa.app.account.AuthFailure
import com.lenterosa.app.account.AuthGateway
import com.lenterosa.app.network.NetworkStatus
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** Punto geográfico de búsqueda. */
data class GeoPoint(val lat: Double, val lng: Double)

/**
 * Zona del piloto (Pasto). Solo es el centro y el radio iniciales de la búsqueda: el código no se
 * restringe por ciudad y la API responde con cualquier coordenada.
 */
object PilotArea {
    val CENTER = GeoPoint(lat = 1.2136, lng = -77.2811)
    const val RADIUS_METERS = 3_000
}

/** Por qué no se pudo cargar algo, en términos que la pantalla traduce a lenguaje cotidiano. */
enum class LoadFailure { NETWORK, SERVER_UNREACHABLE, NOT_FOUND, SERVER }

sealed interface Loaded<out T> {
    data class Success<T>(val value: T) : Loaded<T>
    data class Failed(val failure: LoadFailure) : Loaded<Nothing>
}

/** Ficha de un lugar con sus opiniones más recientes. */
data class PlaceWithReviews(val place: PlaceDetail, val reviews: List<Review>)

/** Resultado de aportar una opinión (RF-04). */
sealed interface ContributionResult {
    data class Published(val review: Review) : ContributionResult

    /** Sin sesión: hay que iniciar sesión antes de opinar. */
    data object NotSignedIn : ContributionResult

    /** La API exige el correo verificado (403). */
    data object EmailNotVerified : ContributionResult

    /** Datos no válidos o con datos de contacto (400); `message` viene de la API en español. */
    data class Rejected(val message: String?, val contactData: Boolean) : ContributionResult

    /** Límite de opiniones (429); `retryAfterSeconds` sale del encabezado `Retry-After`. */
    data class RateLimited(val message: String?, val retryAfterSeconds: Long?) : ContributionResult

    data class Failed(val failure: LoadFailure) : ContributionResult
}

@Singleton
class PlacesRepository @Inject constructor(
    private val api: PlacesApi,
    private val auth: AuthGateway,
    private val network: NetworkStatus,
    private val json: Json,
) {

    suspend fun nearby(center: GeoPoint, query: String?, radiusMeters: Int = PilotArea.RADIUS_METERS): Loaded<List<PlaceSummary>> =
        load { api.nearby(center.lat, center.lng, radiusMeters, query?.trim()?.takeIf { it.isNotEmpty() }).items }

    suspend fun placeWithReviews(id: String): Loaded<PlaceWithReviews> = load {
        coroutineScope {
            val place = async { api.detail(id) }
            val reviews = async { api.reviews(id).items }
            PlaceWithReviews(place.await(), reviews.await())
        }
    }

    suspend fun contribute(placeId: String, draft: ReviewDraft): ContributionResult {
        if (auth.session.value == null) {
            return ContributionResult.NotSignedIn
        }
        return try {
            // Token nuevo: si acaba de verificar el correo, el token anterior aún dice que no.
            val token = auth.idToken(forceRefresh = true)
            ContributionResult.Published(api.contribute(placeId, "Bearer $token", draft))
        } catch (e: HttpException) {
            val problem = problemOf(e)
            when (e.code()) {
                400 -> ContributionResult.Rejected(problem?.detail, contactData = problem?.reason == "CONTACT_DATA")
                401 -> ContributionResult.NotSignedIn
                403 -> ContributionResult.EmailNotVerified
                404 -> ContributionResult.Failed(LoadFailure.NOT_FOUND)
                429 -> ContributionResult.RateLimited(
                    problem?.detail,
                    e.response()?.headers()?.get("Retry-After")?.toLongOrNull(),
                )
                else -> ContributionResult.Failed(LoadFailure.SERVER)
            }
        } catch (e: IOException) {
            ContributionResult.Failed(networkFailure(e))
        } catch (e: AuthException) {
            // Firebase no pudo dar un token: sin sesión válida o sin conexión.
            when (e.failure) {
                AuthFailure.NETWORK -> ContributionResult.Failed(LoadFailure.NETWORK)
                AuthFailure.INVALID_CREDENTIALS -> ContributionResult.NotSignedIn
                else -> ContributionResult.Failed(LoadFailure.SERVER)
            }
        }
    }

    private suspend fun <T> load(block: suspend () -> T): Loaded<T> = try {
        Loaded.Success(block())
    } catch (e: HttpException) {
        Loaded.Failed(if (e.code() == 404) LoadFailure.NOT_FOUND else LoadFailure.SERVER)
    } catch (e: IOException) {
        Loaded.Failed(networkFailure(e))
    }

    // Sin internet es un problema del teléfono; con internet, es que Lente Rosa no respondió.
    private fun networkFailure(e: IOException): LoadFailure {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "No se pudo llamar a la API: ${e.javaClass.simpleName}: ${e.message}")
        }
        return if (network.hasInternet()) LoadFailure.SERVER_UNREACHABLE else LoadFailure.NETWORK
    }

    private fun problemOf(e: HttpException): Problem? = try {
        e.response()?.errorBody()?.string()?.let { json.decodeFromString(Problem.serializer(), it) }
    } catch (_: Exception) {
        null
    }
}

private const val TAG = "PlacesRepository"
