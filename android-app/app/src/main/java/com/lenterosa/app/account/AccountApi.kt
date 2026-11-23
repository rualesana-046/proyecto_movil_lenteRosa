package com.lenterosa.app.account

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.PUT

/** Cuenta en la API de Lente Rosa (RF-02). Siempre con el ID token de Firebase. */
interface AccountApi {

    /** Registra la cuenta la primera vez; es idempotente. */
    @PUT("api/v1/me")
    suspend fun register(@Header("Authorization") bearer: String): AccountDto

    /** Elimina la cuenta en la API: sus opiniones quedan anónimas. */
    @DELETE("api/v1/me")
    suspend fun delete(@Header("Authorization") bearer: String): Response<Unit>
}

@Serializable
data class AccountDto(val uid: String, val emailVerified: Boolean, val registeredAt: String)
