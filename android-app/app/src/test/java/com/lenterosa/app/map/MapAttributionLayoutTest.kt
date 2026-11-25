package com.lenterosa.app.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.lenterosa.app.home.HomeScaffold
import com.lenterosa.app.home.HomeTab
import com.lenterosa.app.home.HomeTags
import com.lenterosa.app.places.Location
import com.lenterosa.app.places.LoadState
import com.lenterosa.app.places.PlaceSummary
import com.lenterosa.app.ui.glass.GlassSurfaceKey
import com.lenterosa.app.ui.theme.LenteRosaTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * La atribución del mapa debe verse siempre completa y nunca quedar tapada por la barra inferior,
 * la tarjeta del lugar, el botón Aportar, los controles del mapa ni ninguna superficie de vidrio
 * (ADR 0009 y 0010). Si una zona translúcida o una tarjeta flotante se superpone, esta prueba falla.
 */
@RunWith(RobolectricTestRunner::class)
// Aplicación simple: la real inicia MapLibre, que usa código nativo que no corre en la JVM.
@Config(sdk = [35], application = android.app.Application::class)
class MapAttributionLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private val place = PlaceSummary(
        id = "p1",
        name = "[FICTICIO] Parque de Prueba Norte con un nombre largo para ocupar dos líneas",
        category = "PARK",
        location = Location(1.22, -77.279),
        distanceMeters = 745,
        comfortIndex = 49,
        comfortLabel = "MIXED_OPINIONS",
    )

    // Peor caso: tarjeta del lugar abierta, "Buscar en esta zona" y un aviso a la vez.
    private val busyState = MapUiState(
        places = LoadState.Ready(listOf(place)),
        selectedId = "p1",
        canSearchHere = true,
        locationMessage = LocationMessage.DISABLED,
    )

    @Test
    fun `la atribución se ve y nada la tapa en un teléfono común`() {
        showMapTab()
        assertAttributionVisibleAndUncovered()
    }

    @Test
    @Config(qualifiers = "w320dp-h568dp")
    fun `la atribución se ve y nada la tapa en una pantalla pequeña`() {
        showMapTab()
        assertAttributionVisibleAndUncovered()
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun `la atribución se ve y nada la tapa con el texto al doble de tamaño`() {
        RuntimeEnvironment.setFontScale(2f)
        showMapTab()
        assertAttributionVisibleAndUncovered()
    }

    @Test
    fun `en la pestaña de la lista no hay mapa ni atribución`() {
        compose.setContent {
            LenteRosaTheme {
                HomeScaffold(HomeTab.LIST, onTabSelected = {}, onOpenPrivacy = {}, onOpenAccount = {}) { _, _, _ -> }
            }
        }
        compose.onNodeWithTag(MapTags.ATTRIBUTION).assertDoesNotExist()
    }

    private fun showMapTab() {
        compose.setContent {
            LenteRosaTheme {
                HomeScaffold(HomeTab.MAP, onTabSelected = {}, onOpenPrivacy = {}, onOpenAccount = {}) { padding, _, _ ->
                    MapTab(
                        state = busyState,
                        contentPadding = padding,
                        onNearMe = {},
                        onSearchHere = {},
                        onRetry = {},
                        onDismissLocationMessage = {},
                        onOpenPlace = {},
                        onContribute = {},
                        onCloseCard = {},
                        // El mapa real (MapLibre) no corre en la JVM; aquí solo importa el espacio que ocupa.
                        map = { modifier -> Box(modifier.background(Color.LightGray)) },
                    )
                }
            }
        }
    }

    @Test
    fun `con novedades sin leer la pestaña Guardados muestra un punto y lo anuncia con texto`() {
        compose.setContent {
            LenteRosaTheme {
                HomeScaffold(HomeTab.MAP, onTabSelected = {}, onOpenPrivacy = {}, onOpenAccount = {}, unreadAlerts = 2) { _, _, _ -> }
            }
        }
        compose.onNodeWithTag(HomeTags.ALERTS_BADGE, useUnmergedTree = true).assertExists()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "2 novedades en tus lugares")).assertExists()
    }

    @Test
    fun `sin novedades no hay punto en la pestaña Guardados`() {
        compose.setContent {
            LenteRosaTheme {
                HomeScaffold(HomeTab.MAP, onTabSelected = {}, onOpenPrivacy = {}, onOpenAccount = {}, unreadAlerts = 0) { _, _, _ -> }
            }
        }
        compose.onNodeWithTag(HomeTags.ALERTS_BADGE, useUnmergedTree = true).assertDoesNotExist()
    }

    private fun assertAttributionVisibleAndUncovered() {
        compose.onNodeWithTag(MapTags.ATTRIBUTION).assertIsDisplayed()
        val attribution = bounds(MapTags.ATTRIBUTION)
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot

        // Completa dentro de la pantalla, no recortada.
        assertTrue("La atribución sale de la pantalla: $attribution en $screen", screen.contains(attribution))
        assertTrue("La atribución no tiene alto visible", attribution.height > 0f)

        // Nada de lo que flota en el mapa ni la barra inferior se superpone.
        listOf(HomeTags.BOTTOM_BAR, MapTags.PLACE_CARD, MapTags.CONTRIBUTE, MapTags.NEAR_ME, MapTags.SEARCH_HERE, MapTags.STATUS)
            .forEach { tag ->
                compose.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
                val other = bounds(tag)
                assertFalse("$tag tapa la atribución: $other sobre $attribution", other.overlaps(attribution))
            }

        // Ninguna superficie de vidrio (barras, tarjetas, avisos) se superpone a la atribución.
        val glass = compose.onAllNodes(SemanticsMatcher.keyIsDefined(GlassSurfaceKey), useUnmergedTree = true).fetchSemanticsNodes()
        assertTrue("No se encontraron superficies de vidrio", glass.isNotEmpty())
        glass.forEach { node ->
            assertFalse("Vidrio sobre la atribución: ${node.boundsInRoot} y $attribution", node.boundsInRoot.overlaps(attribution))
        }

        // El mapa queda por encima de la atribución: la atribución nunca se dibuja sobre el mapa.
        val map = bounds(MapTags.MAP)
        assertTrue("El mapa se mete bajo la atribución: $map y $attribution", map.bottom <= attribution.top)
    }

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    // Rect.contains con un margen de medio píxel por redondeos.
    private fun Rect.contains(other: Rect): Boolean =
        other.left >= left - 0.5f && other.top >= top - 0.5f && other.right <= right + 0.5f && other.bottom <= bottom + 0.5f
}
