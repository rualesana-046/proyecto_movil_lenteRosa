package com.lenterosa.app.places

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContributeViewModelTest {

    private val api = FakePlacesApi()
    private lateinit var viewModel: ContributeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = ContributeViewModel(repository(api))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sin responder las preguntas obligatorias no se envía y se marcan las que faltan`() {
        viewModel.setRating(Question.LIGHTING, 4)

        viewModel.submit("p1")

        val state = viewModel.state.value
        assertTrue(state.showMissing)
        assertEquals(
            listOf(Question.WOMEN_PRESENCE, Question.COMMERCIAL_VISIBILITY, Question.VISIT_PERIOD, Question.NIGHT_EXPERIENCE),
            state.missing,
        )
        assertTrue(api.contributions.isEmpty())
    }

    @Test
    fun `de día con No fui de noche se envía la experiencia nocturna sin valor`() {
        answerDimensions()
        viewModel.setVisitPeriod(VisitPeriod.DAY)
        viewModel.chooseNotAtNight()
        viewModel.setComment("  Buena luz en la Calle 18 # 20-30  ")

        viewModel.submit("p1")

        val sent = api.contributions.single().second
        assertEquals(ReviewDraft(4, 5, 3, null, "DAY", "Buena luz en la Calle 18 # 20-30"), sent)
        assertTrue(viewModel.state.value.published)
    }

    @Test
    fun `de noche la experiencia nocturna es obligatoria y No fui de noche se descarta`() {
        answerDimensions()
        viewModel.setVisitPeriod(VisitPeriod.DAY)
        viewModel.chooseNotAtNight()
        viewModel.setVisitPeriod(VisitPeriod.NIGHT)

        viewModel.submit("p1")

        assertFalse(viewModel.state.value.notAtNight)
        assertEquals(listOf(Question.NIGHT_EXPERIENCE), viewModel.state.value.missing)
        assertTrue(api.contributions.isEmpty())

        viewModel.setRating(Question.NIGHT_EXPERIENCE, 2)
        viewModel.submit("p1")

        assertEquals(ReviewDraft(4, 5, 3, 2, "NIGHT", null), api.contributions.single().second)
    }

    @Test
    fun `calificar la experiencia nocturna anula No fui de noche`() {
        viewModel.chooseNotAtNight()
        viewModel.setRating(Question.NIGHT_EXPERIENCE, 3)

        assertFalse(viewModel.state.value.notAtNight)
        assertEquals(3, viewModel.state.value.nightExperience)
    }

    @Test
    fun `un comentario de más de 500 caracteres no se envía`() {
        answerDimensions()
        viewModel.setVisitPeriod(VisitPeriod.DAY)
        viewModel.chooseNotAtNight()
        viewModel.setComment("a".repeat(501))

        viewModel.submit("p1")

        assertTrue(viewModel.state.value.commentTooLong)
        assertTrue(api.contributions.isEmpty())
    }

    @Test
    fun `el límite de opiniones muestra el mensaje de la API y cuánto falta`() {
        api.failure = httpError(429, """{"detail":"Ya opinaste sobre este lugar."}""", mapOf("Retry-After" to "432000"))
        answerDimensions()
        viewModel.setVisitPeriod(VisitPeriod.DAY)
        viewModel.chooseNotAtNight()

        viewModel.submit("p1")

        val message = viewModel.state.value.message as ContributeMessage.RateLimited
        assertEquals("Ya opinaste sobre este lugar.", message.apiMessage)
        assertEquals(WaitTime(5, WaitUnit.DAYS), message.wait)
        assertFalse(viewModel.state.value.sending)
        assertFalse(viewModel.state.value.published)
    }

    @Test
    fun `después de publicar el formulario vuelve a quedar vacío`() {
        answerDimensions()
        viewModel.setVisitPeriod(VisitPeriod.DAY)
        viewModel.chooseNotAtNight()
        viewModel.submit("p1")

        viewModel.onPublishedHandled()

        assertFalse(viewModel.state.value.published)
        assertNull(viewModel.state.value.lighting)
    }

    private fun answerDimensions() {
        viewModel.setRating(Question.LIGHTING, 4)
        viewModel.setRating(Question.WOMEN_PRESENCE, 5)
        viewModel.setRating(Question.COMMERCIAL_VISIBILITY, 3)
    }
}
