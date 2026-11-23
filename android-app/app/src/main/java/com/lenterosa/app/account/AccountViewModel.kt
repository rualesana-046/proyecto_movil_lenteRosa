package com.lenterosa.app.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Mensajes para la usuaria. La pantalla los muestra con textos en español. */
enum class AccountMessage(val isError: Boolean) {
    VERIFICATION_SENT(false),
    NOT_VERIFIED_YET(false),
    NICKNAME_SAVED(false),
    ACCOUNT_DELETED(false),
    API_UNAVAILABLE(true),
    INVALID_CREDENTIALS(true),
    EMAIL_IN_USE(true),
    WEAK_PASSWORD(true),
    INVALID_EMAIL(true),
    NETWORK(true),
    NO_GOOGLE_ACCOUNT(true),
    SERVER(true),
    UNKNOWN(true),
}

data class AccountUiState(
    val session: Session?,
    val busy: Boolean,
    val message: AccountMessage?,
) {
    /** Cuenta con contraseña y correo sin verificar: no podrá opinar hasta verificarlo (RF-04). */
    val needsEmailVerification: Boolean
        get() = session != null && session.usesPassword && !session.emailVerified
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val auth: AuthGateway,
    private val accounts: AccountRepository,
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<AccountMessage?>(null)

    val state: StateFlow<AccountUiState> = combine(auth.session, busy, message, ::AccountUiState)
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccountUiState(auth.session.value, busy = false, message = null))

    fun signIn(email: String, password: String) = perform {
        auth.signIn(email.trim(), password)
        registerInApi()
    }

    fun signUp(email: String, password: String, nickname: String) = perform {
        auth.signUp(email.trim(), password)
        normalizedNickname(nickname)?.let { auth.updateNickname(it) }
        auth.sendEmailVerification()
        message.value = AccountMessage.VERIFICATION_SENT
        registerInApi()
    }

    fun signInWithGoogle(idToken: String) = perform {
        auth.signInWithGoogle(idToken)
        registerInApi()
    }

    fun resendVerification() = perform {
        auth.sendEmailVerification()
        message.value = AccountMessage.VERIFICATION_SENT
    }

    fun checkVerification() = perform {
        auth.refresh()
        if (auth.session.value?.emailVerified != true) {
            message.value = AccountMessage.NOT_VERIFIED_YET
        }
    }

    fun saveNickname(nickname: String) = perform {
        auth.updateNickname(normalizedNickname(nickname))
        message.value = AccountMessage.NICKNAME_SAVED
    }

    fun signOut() = auth.signOut()

    /** Reautenticación, API (opiniones anónimas) y Firebase, en ese orden. */
    fun deleteAccount(reauthentication: Reauthentication) = perform {
        accounts.deleteAccount(reauthentication)
        message.value = AccountMessage.ACCOUNT_DELETED
    }

    /** Falla al obtener la credencial de Google, fuera del ViewModel. */
    fun reportFailure(failure: AuthFailure) {
        message.value = failure.toMessage()
    }

    fun dismissMessage() {
        message.value = null
    }

    // La sesión sigue activa aunque la API no responda; el registro se reintenta en el próximo inicio de sesión.
    private suspend fun registerInApi() {
        try {
            accounts.registerInApi()
        } catch (e: AuthException) {
            message.value = AccountMessage.API_UNAVAILABLE
        }
    }

    private fun perform(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            message.value = null
            try {
                block()
            } catch (e: AuthException) {
                message.value = e.failure.toMessage()
            } finally {
                busy.value = false
            }
        }
    }

    private fun normalizedNickname(nickname: String): String? =
        nickname.trim().take(MAX_NICKNAME_LENGTH).ifBlank { null }

    private fun AuthFailure.toMessage(): AccountMessage? = when (this) {
        AuthFailure.INVALID_CREDENTIALS -> AccountMessage.INVALID_CREDENTIALS
        AuthFailure.EMAIL_IN_USE -> AccountMessage.EMAIL_IN_USE
        AuthFailure.WEAK_PASSWORD -> AccountMessage.WEAK_PASSWORD
        AuthFailure.INVALID_EMAIL -> AccountMessage.INVALID_EMAIL
        AuthFailure.NETWORK -> AccountMessage.NETWORK
        AuthFailure.NO_GOOGLE_ACCOUNT -> AccountMessage.NO_GOOGLE_ACCOUNT
        AuthFailure.SERVER -> AccountMessage.SERVER
        AuthFailure.CANCELLED -> null
        AuthFailure.UNKNOWN -> AccountMessage.UNKNOWN
    }

    companion object {
        const val MAX_NICKNAME_LENGTH = 40
    }
}
