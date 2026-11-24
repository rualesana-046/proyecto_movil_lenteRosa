package com.lenterosa.app.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.lenterosa.app.ui.glass.ProvideVisualPreferences
import com.lenterosa.app.ui.glass.VisualPreferences
import com.lenterosa.app.ui.theme.LenteRosaTheme
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Bienvenida de tres pasos (RF-11). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class OnboardingTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private var finished = 0

    private fun show(reducedMotion: Boolean = false) {
        compose.setContent {
            LenteRosaTheme {
                ProvideVisualPreferences(VisualPreferences(reducedMotion = reducedMotion)) {
                    OnboardingScreen(onFinish = { finished++ })
                }
            }
        }
    }

    @Test
    fun `muestra el primer paso con su avance en texto`() {
        show()

        compose.onNodeWithText("Cómo se siente un lugar").assertIsDisplayed()
        compose.onNodeWithContentDescription("Paso 1 de 3").assertIsDisplayed()
        compose.onNodeWithTag(OnboardingTags.SKIP).assertIsDisplayed()
    }

    @Test
    fun `con Siguiente se recorren los tres pasos y el último termina la bienvenida`() {
        show()

        compose.onNodeWithTag(OnboardingTags.NEXT).performClick()
        compose.onNodeWithContentDescription("Paso 2 de 3").assertIsDisplayed()
        compose.onNodeWithText("Tu privacidad primero").assertIsDisplayed()

        compose.onNodeWithTag(OnboardingTags.NEXT).performClick()
        compose.onNodeWithContentDescription("Paso 3 de 3").assertIsDisplayed()
        compose.onNodeWithText("Empezar a explorar").assertIsDisplayed()
        // En el último paso ya no hay "Saltar": el botón principal termina.
        compose.onNodeWithTag(OnboardingTags.SKIP).assertDoesNotExist()
        assertEquals(0, finished)

        compose.onNodeWithTag(OnboardingTags.NEXT).performClick()
        assertEquals(1, finished)
    }

    @Test
    fun `Saltar termina la bienvenida desde el primer paso`() {
        show()

        compose.onNodeWithTag(OnboardingTags.SKIP).performClick()

        assertEquals(1, finished)
    }

    @Test
    fun `con Quitar animaciones el paso cambia sin desplazamiento animado`() {
        show(reducedMotion = true)
        compose.mainClock.autoAdvance = false

        compose.onNodeWithTag(OnboardingTags.NEXT).performClick()
        compose.mainClock.advanceTimeBy(50)
        val atOnce = compose.onNodeWithText("Tu privacidad primero").fetchSemanticsNode().boundsInRoot
        compose.mainClock.advanceTimeBy(1_000)
        val settled = compose.onNodeWithText("Tu privacidad primero").fetchSemanticsNode().boundsInRoot

        // El segundo paso ya está en su lugar final a los 50 ms: no hubo desplazamiento animado.
        assertEquals(settled, atOnce)
        compose.onNodeWithContentDescription("Paso 2 de 3").assertIsDisplayed()
    }

    @Test
    fun `la marca de bienvenida completada se guarda y se lee en el teléfono`() = runTest {
        val file = File(folder.root, "ajustes.preferences_pb")
        val store = DataStoreOnboardingStore(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })

        assertFalse(store.completed.first())
        store.markCompleted()

        assertTrue(store.completed.first())
    }
}
