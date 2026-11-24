package com.lenterosa.app.saved

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path

/** Lugares guardados (RF-05) y avisos de cambio (RF-07). Siempre con el ID token de Firebase. */
interface SavedApi {

    @GET("api/v1/me/saved-places")
    suspend fun savedPlaces(@Header("Authorization") bearer: String): List<SavedPlaceDto>

    @PUT("api/v1/me/saved-places/{placeId}")
    suspend fun save(@Header("Authorization") bearer: String, @Path("placeId") placeId: String): Response<Unit>

    @DELETE("api/v1/me/saved-places/{placeId}")
    suspend fun remove(@Header("Authorization") bearer: String, @Path("placeId") placeId: String): Response<Unit>

    @GET("api/v1/me/alerts")
    suspend fun alerts(@Header("Authorization") bearer: String): AlertInbox

    @PUT("api/v1/me/alerts/read")
    suspend fun markAllRead(@Header("Authorization") bearer: String): Response<Unit>
}

@Serializable
data class SavedPlaceDto(val placeId: String, val savedAt: String)

/** Aviso de que cambió la percepción de un lugar guardado. Las etiquetas son códigos de la API. */
@Serializable
data class AlertDto(
    val id: String,
    val placeId: String,
    val previousLabel: String,
    val previousIndex: Int? = null,
    val newLabel: String,
    val newIndex: Int? = null,
    val createdAt: String,
    val read: Boolean,
)

@Serializable
data class AlertInbox(val unreadCount: Int, val items: List<AlertDto>)
