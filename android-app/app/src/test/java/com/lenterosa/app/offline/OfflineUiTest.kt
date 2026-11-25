package com.lenterosa.app.offline

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.lenterosa.app.R
import com.lenterosa.app.map.MapTags
import com.lenterosa.app.map.PlaceMapCard
import com.lenterosa.app.places.ContributeScreen
import com.lenterosa.app.places.ContributeTags
import com.lenterosa.app.places.ContributeViewModel
import com.lenterosa.app.places.DetailTags
import com.lenterosa.app.places.FakePlacesApi
import com.lenterosa.app.places.LoadFailure
import com.lenterosa.app.places.PlaceDetailContent
import com.lenterosa.app.places.PlaceWithReviews
import com.lenterosa.app.places.StaleBanner
import com.lenterosa.app.places.StaleTags
import com.lenterosa.app.places.Staleness
import com.lenterosa.app.places.placeDetail
import com.lenterosa.app.places.placeSummary
import com.lenterosa.app.places.repository
import com.lenterosa.app.places.review
import com.lenterosa.app.places.session
import com.lenterosa.app.ui.theme.LenteRosaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** RF-10 en pantalla: aviso de datos guardados y Aportar sin conexión. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class OfflineUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val now = 1_000_000_000_000L
    private val hour = 60L * 60 * 1000

    @Test
    fun `el aviso dice hace cuánto se guardaron los datos y que no hay conexión`() {
        var retries = 0
        compose.setContent {
            LenteRosaTheme { StaleBanner(Staleness(now - 3 * hour, LoadFailure.NETWORK), onRetry = { retries++ }, nowMillis = now) }
        }

        compose.onNodeWithText("Datos guardados hace 3 horas · Sin conexión").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.places_retry)).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `si el servidor falla el aviso no dice que no hay conexión`() {
        compose.setContent {
            LenteRosaTheme { StaleBanner(Staleness(now - 2 * 24 * hour, LoadFailure.SERVER_UNREACHABLE), onRetry = {}, nowMillis = now) }
        }

        compose.onNodeWithText("Datos guardados hace 2 días · No pudimos actualizar").assertIsDisplayed()
    }

    @Test
    fun `datos de hace segundos se describen como hace un momento`() {
        compose.setContent {
            LenteRosaTheme { StaleBanner(Staleness(now - 20_000, LoadFailure.NETWORK), onRetry = {}, nowMillis = now) }
        }

        compose.onNodeWithText("Datos guardados hace un momento · Sin conexión").assertIsDisplayed()
    }

    @Test
    fun `sin conexión la ficha muestra el aviso y Aportar queda desactivado con el motivo`() {
        showDetail(online = false, stale = Staleness(now - hour, LoadFailure.NETWORK))

        compose.onNodeWithTag(StaleTags.BANNER).assertIsDisplayed()
        scrollList(hasTestTag(DetailTags.CONTRIBUTE))
        compose.onNodeWithTag(DetailTags.CONTRIBUTE).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.offline_contribute_needs_connection)).assertIsDisplayed()
    }

    @Test
    fun `con conexión la ficha no muestra el aviso y se puede aportar`() {
        showDetail(online = true, stale = null)

        compose.onNodeWithTag(StaleTags.BANNER).assertDoesNotExist()
        scrollList(hasText(context.getString(R.string.detail_contribute)))
        compose.onNodeWithText(context.getString(R.string.offline_contribute_needs_connection)).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.detail_contribute)).assertIsEnabled()
    }

    @Test
    fun `sin conexión Aportar de la tarjeta del mapa queda desactivado y se explica`() {
        var contributions = 0
        compose.setContent {
            LenteRosaTheme {
                PlaceMapCard(placeSummary(), saved = false, online = false, onToggleSaved = {}, hazeState = null,
                    onOpen = {}, onContribute = { contributions++ }, onClose = {})
            }
        }

        compose.onNodeWithTag(MapTags.CONTRIBUTE).assertIsNotEnabled().performClick()
        compose.onNodeWithText(context.getString(R.string.offline_contribute_needs_connection)).assertIsDisplayed()
        assertEquals(0, contributions)
    }

    @Test
    fun `sin conexión el formulario no envía ni guarda la opinión para después`() {
        val api = FakePlacesApi()
        val viewModel = ContributeViewModel(repository(api), FakeConnectivity(initial = false))
        compose.setContent {
            LenteRosaTheme { ContributeScreen(viewModel, "p1", "[FICTICIO] Parque", onBack = {}, onPublished = {}, onOpenAccount = {}) }
        }

        compose.onNodeWithTag(ContributeTags.OFFLINE).assertExists()
        compose.onNodeWithTag(ContributeTags.SUBMIT).assertIsNotEnabled()
        viewModel.submit("p1")
        compose.waitForIdle()
        assertTrue(api.contributions.isEmpty())
    }

    @Test
    fun `al volver la conexión el formulario se puede enviar`() {
        val connectivity = FakeConnectivity(initial = false)
        val viewModel = ContributeViewModel(repository(FakePlacesApi()), connectivity)
        compose.setContent {
            LenteRosaTheme { ContributeScreen(viewModel, "p1", "[FICTICIO] Parque", onBack = {}, onPublished = {}, onOpenAccount = {}) }
        }

        connectivity.online.value = true

        compose.onNodeWithTag(ContributeTags.OFFLINE).assertDoesNotExist()
        compose.onNodeWithTag(ContributeTags.SUBMIT).assertIsEnabled()
    }

    private fun scrollList(target: SemanticsMatcher) {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(target)
    }

    private fun showDetail(online: Boolean, stale: Staleness?) {
        compose.setContent {
            LenteRosaTheme {
                PlaceDetailContent(
                    data = PlaceWithReviews(placeDetail(), listOf(review())),
                    stale = stale,
                    online = online,
                    session = session(),
                    padding = PaddingValues(),
                    onContribute = { _, _ -> },
                    onOpenAccount = {},
                    onRetry = {},
                )
            }
        }
    }
}
