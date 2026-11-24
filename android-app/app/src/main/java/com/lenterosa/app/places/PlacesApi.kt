package com.lenterosa.app.places

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Lugares y opiniones en la API de Lente Rosa: consulta pública (RF-01) y aportar con sesión (RF-04). */
interface PlacesApi {

    /** Las coordenadas son las del centro de búsqueda; la API no las guarda (RNF-01). */
    @GET("api/v1/places")
    suspend fun nearby(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusMeters") radiusMeters: Int,
        @Query("q") query: String?,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): Page<PlaceSummary>

    @GET("api/v1/places/{id}")
    suspend fun detail(@Path("id") id: String): PlaceDetail

    @GET("api/v1/places/{id}/reviews")
    suspend fun reviews(@Path("id") id: String, @Query("page") page: Int = 0, @Query("size") size: Int = 20): Page<Review>

    @POST("api/v1/places/{id}/reviews")
    suspend fun contribute(
        @Path("id") id: String,
        @Header("Authorization") bearer: String,
        @Body review: ReviewDraft,
    ): Review
}

@Serializable
data class Page<T>(val items: List<T>, val page: Int, val size: Int, val totalItems: Long, val totalPages: Int)

@Serializable
data class Location(val lat: Double, val lng: Double)

@Serializable
data class PlaceSummary(
    val id: String,
    val name: String,
    val category: String,
    val address: String? = null,
    val location: Location,
    val distanceMeters: Long,
    val comfortIndex: Int? = null,
    val comfortLabel: String,
)

/** Promedios crudos por dimensión, de 1 a 5. Nulo significa "Sin datos", nunca 0. */
@Serializable
data class DimensionAverages(
    val lighting: Double? = null,
    val womenPresence: Double? = null,
    val commercialVisibility: Double? = null,
    val nightExperience: Double? = null,
)

@Serializable
data class Comfort(
    val index: Int? = null,
    val label: String,
    val uncertainty: String,
    val nightDataMissing: Boolean,
)

@Serializable
data class PlaceDetail(
    val id: String,
    val name: String,
    val category: String,
    val address: String? = null,
    val location: Location,
    val reviewCount: Long,
    val lastReviewAt: String? = null,
    val averages: DimensionAverages,
    val comfort: Comfort,
)

/** Opinión anónima: la API nunca envía datos de la autora. */
@Serializable
data class Review(
    val id: String,
    val lighting: Int,
    val womenPresence: Int,
    val commercialVisibility: Int,
    val nightExperience: Int? = null,
    val visitPeriod: String,
    val comment: String? = null,
    val createdAt: String,
)

/** Cuerpo para aportar una opinión. `nightExperience` es nula si fue de día y eligió "No fui de noche". */
@Serializable
data class ReviewDraft(
    val lighting: Int,
    val womenPresence: Int,
    val commercialVisibility: Int,
    val nightExperience: Int?,
    val visitPeriod: String,
    val comment: String?,
)

/** Error en formato RFC 9457; `detail` ya viene en español para mostrarlo tal cual. */
@Serializable
data class Problem(val title: String? = null, val detail: String? = null, val reason: String? = null)
