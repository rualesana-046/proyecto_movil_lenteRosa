package com.lenterosa.app.account

import kotlinx.coroutines.flow.StateFlow

/**
 * Sesión de la usuaria según Firebase Authentication. El apodo vive solo en Firebase
 * (displayName); la API nunca lo recibe.
 */
data class Session(
    val uid: String,
    val email: String?,
    val emailVerified: Boolean,
    val nickname: String?,
    val usesPassword: Boolean,
    val usesGoogle: Boolean,
)

/** Fallas de autenticación, sin tipos de Firebase, para mostrarlas en lenguaje cotidiano. */
enum class AuthFailure {
    INVALID_CREDENTIALS,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    INVALID_EMAIL,
    NETWORK,
    SERVER_UNREACHABLE,
    NO_GOOGLE_ACCOUNT,
    CANCELLED,
    SERVER,
    UNKNOWN,
}

class AuthException(val failure: AuthFailure, cause: Throwable? = null) : Exception(failure.name, cause)

/** Operaciones de identidad. La implementación real usa Firebase; las pruebas usan un doble. */
interface AuthGateway {

    val session: StateFlow<Session?>

    suspend fun signIn(email: String, password: String)

    suspend fun signUp(email: String, password: String)

    suspend fun signInWithGoogle(idToken: String)

    suspend fun sendEmailVerification()

    /** Vuelve a leer la usuaria desde Firebase, por ejemplo después de verificar el correo. */
    suspend fun refresh()

    suspend fun updateNickname(nickname: String?)

    /** ID token vigente para la API; con `forceRefresh` se pide uno nuevo a Firebase. */
    suspend fun idToken(forceRefresh: Boolean = false): String

    suspend fun reauthenticateWithPassword(password: String)

    suspend fun reauthenticateWithGoogle(idToken: String)

    suspend fun deleteFirebaseAccount()

    fun signOut()
}
