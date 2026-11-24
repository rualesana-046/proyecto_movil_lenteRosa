package com.lenterosa.app.places

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlacesRepositoryTest {

    private val api = FakePlacesApi()
    private val draft = ReviewDraft(4, 5, 3, null, "DAY", null)

    @Test
    fun `la búsqueda envía el nombre sin espacios y omite una búsqueda vacía`() = runTest {
        val repository = repository(api)

        repository.nearby(PilotArea.CENTER, "  parque ")
        repository.nearby(PilotArea.CENTER, "   ")

        assertEquals(listOf("parque", null), api.nearbyQueries)
    }

    @Test
    fun `sin internet es un error de red y con internet es que Lente Rosa no respondió`() = runTest {
        api.failure = offline()

        assertEquals(Loaded.Failed(LoadFailure.NETWORK), repository(api, hasInternet = false).nearby(PilotArea.CENTER, null))
        assertEquals(Loaded.Failed(LoadFailure.SERVER_UNREACHABLE), repository(api, hasInternet = true).nearby(PilotArea.CENTER, null))
    }

    @Test
    fun `un lugar inexistente se informa como no encontrado`() = runTest {
        api.failure = httpError(404)

        assertEquals(Loaded.Failed(LoadFailure.NOT_FOUND), repository(api).placeWithReviews("p1"))
    }

    @Test
    fun `la ficha trae el lugar y sus opiniones`() = runTest {
        val result = repository(api).placeWithReviews("p1") as Loaded.Success

        assertEquals("p1", result.value.place.id)
        assertEquals(1, result.value.reviews.size)
    }

    @Test
    fun `aportar usa un token recién emitido`() = runTest {
        val auth = FakeSessionGateway()

        val result = repository(api, auth).contribute("p1", draft)

        assertTrue(result is ContributionResult.Published)
        assertEquals(listOf(true), auth.tokenRequests)
        assertEquals("Bearer token-1", api.contributions.single().first)
    }

    @Test
    fun `sin sesión no se llama a la API`() = runTest {
        val result = repository(api, FakeSessionGateway(initial = null)).contribute("p1", draft)

        assertEquals(ContributionResult.NotSignedIn, result)
        assertTrue(api.contributions.isEmpty())
    }

    @Test
    fun `un comentario con datos de contacto trae el mensaje de la API`() = runTest {
        api.failure = httpError(400, """{"title":"Opinión no válida","detail":"No incluyas datos de contacto.","reason":"CONTACT_DATA"}""")

        val result = repository(api).contribute("p1", draft)

        assertEquals(ContributionResult.Rejected("No incluyas datos de contacto.", contactData = true), result)
    }

    @Test
    fun `el correo sin verificar responde 403`() = runTest {
        api.failure = httpError(403, """{"detail":"Verifica tu correo"}""")

        assertEquals(ContributionResult.EmailNotVerified, repository(api).contribute("p1", draft))
    }

    @Test
    fun `el límite de opiniones lee el encabezado Retry-After`() = runTest {
        api.failure = httpError(429, """{"detail":"Ya opinaste."}""", mapOf("Retry-After" to "432000"))

        assertEquals(ContributionResult.RateLimited("Ya opinaste.", 432_000), repository(api).contribute("p1", draft))
    }

    @Test
    fun `un cuerpo de error que no es JSON no rompe la app`() = runTest {
        api.failure = httpError(400, "<html>")

        assertEquals(ContributionResult.Rejected(null, contactData = false), repository(api).contribute("p1", draft))
    }
}
