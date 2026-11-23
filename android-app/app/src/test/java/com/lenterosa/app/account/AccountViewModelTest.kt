package com.lenterosa.app.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private val auth = FakeAuthGateway()
    private val api = FakeAccountApi(auth.events)
    private lateinit var viewModel: AccountViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = AccountViewModel(auth, AccountRepository(auth, api))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `iniciar sesión registra la cuenta en la API con el token de Firebase`() {
        viewModel.signIn("  ana@ejemplo.com ", "secreta")

        assertEquals("ana@ejemplo.com", auth.lastEmail)
        assertEquals(listOf("Bearer token-1"), api.registered)
        assertEquals("uid-1", viewModel.state.value.session?.uid)
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `credenciales incorrectas muestran un mensaje y no llaman a la API`() {
        auth.failNext = AuthFailure.INVALID_CREDENTIALS

        viewModel.signIn("ana@ejemplo.com", "mala")

        assertEquals(AccountMessage.INVALID_CREDENTIALS, viewModel.state.value.message)
        assertTrue(api.registered.isEmpty())
        assertFalse(viewModel.state.value.busy)
    }

    @Test
    fun `crear cuenta guarda el apodo solo en Firebase y envía la verificación`() {
        viewModel.signUp("ana@ejemplo.com", "secreta", "  Ana  ")

        assertEquals("Ana", auth.session.value?.nickname)
        assertEquals(1, auth.verificationsSent)
        assertEquals(AccountMessage.VERIFICATION_SENT, viewModel.state.value.message)
        assertTrue(viewModel.state.value.needsEmailVerification)
        // La API solo recibe el token: el apodo nunca viaja en la petición.
        assertEquals(listOf("Bearer token-1"), api.registered)
    }

    @Test
    fun `si la API no responde la sesión sigue activa con un aviso`() {
        api.failRegister = true

        viewModel.signIn("ana@ejemplo.com", "secreta")

        assertEquals("uid-1", viewModel.state.value.session?.uid)
        assertEquals(AccountMessage.API_UNAVAILABLE, viewModel.state.value.message)
    }

    @Test
    fun `cancelar el selector de Google no muestra error`() {
        viewModel.reportFailure(AuthFailure.CANCELLED)

        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `eliminar la cuenta reautentica, borra en la API con token nuevo y luego en Firebase`() {
        viewModel.signIn("ana@ejemplo.com", "secreta")

        viewModel.deleteAccount(Reauthentication.WithPassword("secreta"))

        assertEquals(listOf("reauth:password", "token:refresh", "api:delete", "firebase:delete", "signOut"), auth.events)
        assertNull(viewModel.state.value.session)
        assertEquals(AccountMessage.ACCOUNT_DELETED, viewModel.state.value.message)
    }

    @Test
    fun `si la API falla al eliminar, la cuenta de Firebase no se toca`() {
        viewModel.signIn("ana@ejemplo.com", "secreta")
        api.failDelete = true

        viewModel.deleteAccount(Reauthentication.WithPassword("secreta"))

        assertFalse("firebase:delete" in auth.events)
        assertEquals("uid-1", viewModel.state.value.session?.uid)
        assertEquals(AccountMessage.SERVER, viewModel.state.value.message)
    }

    @Test
    fun `una reautenticación fallida no llega a la API`() {
        viewModel.signIn("ana@ejemplo.com", "secreta")
        auth.failNext = AuthFailure.INVALID_CREDENTIALS

        viewModel.deleteAccount(Reauthentication.WithPassword("mala"))

        assertTrue(api.deleted.isEmpty())
        assertEquals(AccountMessage.INVALID_CREDENTIALS, viewModel.state.value.message)
    }

    private class FakeAuthGateway : AuthGateway {
        val events = mutableListOf<String>()
        var failNext: AuthFailure? = null
        var lastEmail: String? = null
        var verificationsSent = 0
        private val current = MutableStateFlow<Session?>(null)
        override val session: StateFlow<Session?> = current

        private fun maybeFail() {
            failNext?.let { failNext = null; throw AuthException(it) }
        }

        override suspend fun signIn(email: String, password: String) {
            maybeFail()
            lastEmail = email
            current.value = Session("uid-1", email, emailVerified = true, nickname = null, usesPassword = true, usesGoogle = false)
        }

        override suspend fun signUp(email: String, password: String) {
            maybeFail()
            current.value = Session("uid-1", email, emailVerified = false, nickname = null, usesPassword = true, usesGoogle = false)
        }

        override suspend fun signInWithGoogle(idToken: String) {
            maybeFail()
            current.value = Session("uid-1", "ana@gmail.com", emailVerified = true, nickname = null, usesPassword = false, usesGoogle = true)
        }

        override suspend fun sendEmailVerification() {
            verificationsSent++
        }

        override suspend fun refresh() = Unit

        override suspend fun updateNickname(nickname: String?) {
            current.value = current.value?.copy(nickname = nickname)
        }

        override suspend fun idToken(forceRefresh: Boolean): String {
            if (forceRefresh) events += "token:refresh"
            return "token-1"
        }

        override suspend fun reauthenticateWithPassword(password: String) {
            maybeFail()
            events += "reauth:password"
        }

        override suspend fun reauthenticateWithGoogle(idToken: String) {
            maybeFail()
            events += "reauth:google"
        }

        override suspend fun deleteFirebaseAccount() {
            events += "firebase:delete"
        }

        override fun signOut() {
            events += "signOut"
            current.value = null
        }
    }

    // Comparte la lista de eventos con la autenticación para comprobar el orden real de las llamadas.
    private class FakeAccountApi(private val events: MutableList<String>) : AccountApi {
        val registered = mutableListOf<String>()
        val deleted = mutableListOf<String>()
        var failRegister = false
        var failDelete = false

        override suspend fun register(bearer: String): AccountDto {
            if (failRegister) throw java.io.IOException("sin conexión")
            registered += bearer
            return AccountDto("uid-1", emailVerified = true, registeredAt = "2026-11-01T12:00:00Z")
        }

        override suspend fun delete(bearer: String): Response<Unit> {
            if (failDelete) return Response.error(500, "".toResponseBody(null))
            deleted += bearer
            events += "api:delete"
            return Response.success(204, Unit)
        }
    }
}
