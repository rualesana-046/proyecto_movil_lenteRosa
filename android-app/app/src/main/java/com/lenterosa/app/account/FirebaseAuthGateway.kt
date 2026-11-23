package com.lenterosa.app.account

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseAuthGateway @Inject constructor(private val auth: FirebaseAuth) : AuthGateway {

    private val current = MutableStateFlow(auth.currentUser?.toSession())

    override val session: StateFlow<Session?> = current.asStateFlow()

    init {
        auth.addAuthStateListener { current.value = it.currentUser?.toSession() }
    }

    override suspend fun signIn(email: String, password: String): Unit = firebase {
        auth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    override suspend fun signUp(email: String, password: String): Unit = firebase {
        auth.createUserWithEmailAndPassword(email, password).await()
        Unit
    }

    override suspend fun signInWithGoogle(idToken: String): Unit = firebase {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        Unit
    }

    override suspend fun sendEmailVerification(): Unit = firebase {
        requireUser().sendEmailVerification().await()
        Unit
    }

    override suspend fun refresh() = firebase {
        requireUser().reload().await()
        current.value = auth.currentUser?.toSession()
    }

    override suspend fun updateNickname(nickname: String?) = firebase {
        requireUser().updateProfile(userProfileChangeRequest { displayName = nickname }).await()
        current.value = auth.currentUser?.toSession()
    }

    override suspend fun idToken(forceRefresh: Boolean): String = firebase {
        requireUser().getIdToken(forceRefresh).await().token ?: throw AuthException(AuthFailure.UNKNOWN)
    }

    override suspend fun reauthenticateWithPassword(password: String): Unit = firebase {
        val user = requireUser()
        val email = user.email ?: throw AuthException(AuthFailure.INVALID_CREDENTIALS)
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
        Unit
    }

    override suspend fun reauthenticateWithGoogle(idToken: String): Unit = firebase {
        requireUser().reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()
        Unit
    }

    override suspend fun deleteFirebaseAccount(): Unit = firebase {
        requireUser().delete().await()
        Unit
    }

    override fun signOut() = auth.signOut()

    private fun requireUser(): FirebaseUser = auth.currentUser ?: throw AuthException(AuthFailure.INVALID_CREDENTIALS)

    // Traduce las excepciones de Firebase a fallas propias, sin exponer detalles técnicos.
    private suspend fun <T> firebase(block: suspend () -> T): T = try {
        block()
    } catch (e: AuthException) {
        throw e
    } catch (e: FirebaseAuthWeakPasswordException) {
        throw AuthException(AuthFailure.WEAK_PASSWORD, e)
    } catch (e: FirebaseAuthUserCollisionException) {
        throw AuthException(AuthFailure.EMAIL_IN_USE, e)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        val failure = if (e.errorCode == "ERROR_INVALID_EMAIL") AuthFailure.INVALID_EMAIL else AuthFailure.INVALID_CREDENTIALS
        throw AuthException(failure, e)
    } catch (e: FirebaseAuthInvalidUserException) {
        throw AuthException(AuthFailure.INVALID_CREDENTIALS, e)
    } catch (e: FirebaseNetworkException) {
        throw AuthException(AuthFailure.NETWORK, e)
    } catch (e: Exception) {
        throw AuthException(AuthFailure.UNKNOWN, e)
    }

    private fun FirebaseUser.toSession() = Session(
        uid = uid,
        email = email,
        emailVerified = isEmailVerified,
        nickname = displayName?.takeIf { it.isNotBlank() },
        usesPassword = providerData.any { it.providerId == EmailAuthProvider.PROVIDER_ID },
        usesGoogle = providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID },
    )
}
