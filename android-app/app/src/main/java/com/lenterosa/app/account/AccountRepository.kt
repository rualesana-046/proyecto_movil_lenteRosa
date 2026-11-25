package com.lenterosa.app.account

import android.util.Log
import com.lenterosa.app.BuildConfig
import com.lenterosa.app.network.NetworkStatus
import com.lenterosa.app.offline.OfflineCache
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

/** Cómo confirma la usuaria su identidad antes de eliminar la cuenta. */
sealed interface Reauthentication {
    data class WithPassword(val password: String) : Reauthentication
    data class WithGoogle(val idToken: String) : Reauthentication
}

/** Coordina Firebase y la API para registrar y eliminar la cuenta. */
@Singleton
class AccountRepository @Inject constructor(
    private val auth: AuthGateway,
    private val api: AccountApi,
    private val network: NetworkStatus,
    private val cache: OfflineCache,
) {

    /** Al cerrar sesión o eliminar la cuenta, no queda en el teléfono nada de la cuenta (RF-10). */
    suspend fun clearLocalAccountData() {
        cache.clearSaved()
    }


    /** Registra la cuenta en la API después de iniciar sesión. */
    suspend fun registerInApi() {
        api(onHttpError = AuthFailure.SERVER) { api.register(bearer()) }
    }

    /**
     * Elimina la cuenta en este orden: reautenticación, API (opiniones anónimas) y Firebase.
     * Si la API falla, la cuenta de Firebase no se toca, para no dejar datos sin dueña identificable.
     */
    suspend fun deleteAccount(reauthentication: Reauthentication) {
        when (reauthentication) {
            is Reauthentication.WithPassword -> auth.reauthenticateWithPassword(reauthentication.password)
            is Reauthentication.WithGoogle -> auth.reauthenticateWithGoogle(reauthentication.idToken)
        }
        val response = api(onHttpError = AuthFailure.SERVER) { api.delete(bearer(forceRefresh = true)) }
        if (!response.isSuccessful) {
            throw AuthException(AuthFailure.SERVER)
        }
        auth.deleteFirebaseAccount()
        auth.signOut()
        clearLocalAccountData()
    }

    private suspend fun bearer(forceRefresh: Boolean = false) = "Bearer ${auth.idToken(forceRefresh)}"

    private suspend fun <T> api(onHttpError: AuthFailure, block: suspend () -> T): T = try {
        block()
    } catch (e: AuthException) {
        throw e
    } catch (e: HttpException) {
        throw AuthException(onHttpError, e)
    } catch (e: IOException) {
        // Sin internet es un problema del teléfono; con internet, es que Lente Rosa no respondió.
        val failure = if (network.hasInternet()) AuthFailure.SERVER_UNREACHABLE else AuthFailure.NETWORK
        if (BuildConfig.DEBUG) {
            Log.w(TAG, "No se pudo llamar a la API: ${e.javaClass.simpleName}: ${e.message}")
        }
        throw AuthException(failure, e)
    }
}

private const val TAG = "AccountRepository"
