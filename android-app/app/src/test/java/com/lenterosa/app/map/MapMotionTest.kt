package com.lenterosa.app.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.lenterosa.app.places.LoadState
import com.lenterosa.app.places.Location
import com.lenterosa.app.places.PlaceSummary
import com.lenterosa.app.ui.glass.ProvideVisualPreferences
import com.lenterosa.app.ui.glass.VisualPreferences
import com.lenterosa.app.ui.theme.LenteRosaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Con "Quitar animaciones", la tarjeta del lugar aparece y desaparece al instante (RNF-02, ADR 0010). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class MapMotionTest {

    @get:Rule
    val compose = createComposeRule()

    private val place = PlaceSummary(
        id = "p1", name = "[FICTICIO] Parque", category = "PARK", location = Location(1.22, -77.279),
        distanceMeters = 745, comfortIndex = 49, comfortLabel = "MIXED_OPINIONS",
    )

    private var selected by mutableStateOf<String?>(null)

    @Test
    fun `con Quitar animaciones la tarjeta aparece sin esperar`() {
        show(reducedMotion = true)
        compose.mainClock.autoAdvance = false

        select("p1")
        compose.mainClock.advanceTimeBy(INSTANT_MS)
        compose.onNodeWithTag(MapTags.PLACE_CARD).assertIsDisplayed()
        val atOnce = compose.onNodeWithTag(MapTags.PLACE_CARD).fetchSemanticsNode().boundsInRoot
        compose.mainClock.advanceTimeBy(1_000)
        val settled = compose.onNodeWithTag(MapTags.PLACE_CARD).fetchSemanticsNode().boundsInRoot

        // Ya está en su lugar final: no hubo deslizamiento.
        assertEquals(settled, atOnce)
    }

    @Test
    fun `con animaciones la tarjeta entra en movimiento y termina en su lugar`() {
        show(reducedMotion = false)
        compose.mainClock.autoAdvance = false

        select("p1")
        compose.mainClock.advanceTimeBy(INSTANT_MS)
        val start = compose.onNodeWithTag(MapTags.PLACE_CARD).fetchSemanticsNode().boundsInRoot.top
        compose.mainClock.advanceTimeBy(1_000)
        val end = compose.onNodeWithTag(MapTags.PLACE_CARD).fetchSemanticsNode().boundsInRoot.top

        assertTrue("La tarjeta debería subir: empieza en $start y termina en $end", start > end)
    }

    @Test
    fun `con Quitar animaciones la tarjeta se va sin esperar`() {
        selected = "p1"
        show(reducedMotion = true)
        compose.mainClock.autoAdvance = false

        select(null)
        compose.mainClock.advanceTimeBy(INSTANT_MS)

        compose.onNodeWithTag(MapTags.PLACE_CARD).assertDoesNotExist()
    }

    private companion object {
        // Unos tres cuadros: menos que la animación más corta de la app (180 ms).
        const val INSTANT_MS = 50L
    }

    // El cambio se aplica en el hilo de la UI y se notifica de inmediato, sin esperar al reloj.
    private fun select(id: String?) {
        compose.runOnUiThread {
            selected = id
            Snapshot.sendApplyNotifications()
        }
    }

    private fun show(reducedMotion: Boolean) {
        compose.setContent {
            LenteRosaTheme {
                ProvideVisualPreferences(VisualPreferences(reducedMotion = reducedMotion)) {
                    MapTab(
                        state = MapUiState(places = LoadState.Ready(listOf(place)), selectedId = selected),
                        contentPadding = PaddingValues(),
                        onNearMe = {},
                        onSearchHere = {},
                        onRetry = {},
                        onDismissLocationMessage = {},
                        onOpenPlace = {},
                        onContribute = {},
                        onCloseCard = {},
                        map = { modifier -> Box(modifier.background(Color.LightGray)) },
                    )
                }
            }
        }
    }
}
