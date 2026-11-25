package com.lenterosa.app.discreet

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.core.app.ApplicationProvider
import com.lenterosa.app.R
import com.lenterosa.app.home.HomeScaffold
import com.lenterosa.app.home.HomeTab
import com.lenterosa.app.home.HomeTags
import com.lenterosa.app.ui.glass.GlassSurfaceKey
import com.lenterosa.app.ui.glass.ProvideVisualPreferences
import com.lenterosa.app.ui.glass.VisualPreferences
import com.lenterosa.app.ui.theme.LenteRosaTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Pantalla, entradas y nombres neutros del Modo discreto (RF-08). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class DiscreetScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var clicks = mutableListOf<String>()

    private fun show(state: DiscreetUiState = DiscreetUiState()) {
        compose.setContent {
            LenteRosaTheme {
                ProvideVisualPreferences(VisualPreferences(discreetMode = true)) {
                    DiscreetScreen(
                        state = state,
                        colors = DiscreetColors.Light,
                        onSendLocation = { clicks += "send" },
                        onFakeCall = { clicks += "call" },
                        onMark = { clicks += "mark" },
                        onUndo = { clicks += "undo" },
                        onDismissStatus = {},
                        onOpenSettings = {},
                        onCallEmergency = { clicks += "123" },
                        onExit = {},
                    )
                }
            }
        }
    }

    @Test
    fun `las tres acciones se hacen con un solo toque`() {
        show()

        compose.onNodeWithTag(DiscreetTags.SEND).performClick()
        compose.onNodeWithTag(DiscreetTags.FAKE_CALL).performClick()
        compose.onNodeWithTag(DiscreetTags.MARK).performClick()

        assertEquals(listOf("send", "call", "mark"), clicks)
    }

    @Test
    fun `la pantalla no muestra el nombre de la app ni usa liquid glass`() {
        show(DiscreetUiState(settings = DiscreetSettings(contacts = listOf(TrustedContact("Sofía", "3001234567")))))

        compose.onNodeWithText("Lente Rosa").assertDoesNotExist()
        compose.onNodeWithText("Se abre un mensaje para Sofía").assertIsDisplayed()
        assertTrue(compose.onAllNodes(SemanticsMatcher.keyIsDefined(GlassSurfaceKey)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `el aviso de Deshacer aparece y llama a deshacer`() {
        show(DiscreetUiState(undo = UndoOffer.Mark("m1", withLocation = true)))

        compose.onNodeWithText("Marca guardada en tu teléfono.").assertIsDisplayed()
        compose.onNodeWithTag(DiscreetTags.UNDO).performClick()

        assertEquals(listOf("undo"), clicks)
    }

    @Test
    fun `con el aviso de Deshacer y un estado a la vez, el acceso al 123 sigue visible y sin tapar`() {
        show(DiscreetUiState(call = FakeCall.Scheduled(8), undo = UndoOffer.Call(10), sendStatus = SendStatus.MESSAGE_READY))

        compose.onNodeWithTag(DiscreetTags.EMERGENCY).assertIsDisplayed()
        val emergency = compose.onNodeWithTag(DiscreetTags.EMERGENCY).fetchSemanticsNode().boundsInRoot
        val undo = compose.onNodeWithTag(DiscreetTags.UNDO).fetchSemanticsNode().boundsInRoot
        assertFalse("Deshacer tapa el 123: $undo sobre $emergency", undo.overlaps(emergency))
        // Una sola cuenta regresiva, la del aviso, con los segundos que faltan.
        compose.onNodeWithText("La llamada sonará en 8 s.").assertIsDisplayed()
        compose.onNodeWithText("La llamada sonará en 10 s.").assertDoesNotExist()
    }

    @Test
    fun `en la llamada simulada se puede tocar el texto de Contestar y de Colgar, no solo el círculo`() {
        var answered = 0
        var hungUp = 0
        compose.setContent {
            LenteRosaTheme { FakeCallScreen(FakeCall.Ringing("Mamá"), onAnswer = { answered++ }, onHangUp = { hungUp++ }) }
        }

        compose.onNodeWithText("Contestar").performClick()
        compose.onNodeWithText("Colgar").performClick()

        assertEquals(1, answered)
        assertEquals(1, hungUp)
    }

    @Test
    fun `la ubicación aproximada se avisa como menos exacta`() {
        show(DiscreetUiState(sendStatus = SendStatus.MESSAGE_READY_APPROXIMATE))

        compose.onNodeWithTag(DiscreetTags.STATUS).assertIsDisplayed()
        compose.onNodeWithText("Tu mensaje está listo con tu ubicación aproximada, que es menos exacta. Tú decides si lo envías.")
            .assertIsDisplayed()
    }

    @Test
    fun `mantener presionado el título del inicio abre el Modo discreto`() {
        var opened = 0
        compose.setContent {
            LenteRosaTheme {
                HomeScaffold(HomeTab.LIST, onTabSelected = {}, onOpenPrivacy = {}, onOpenAccount = {}, onOpenDiscreet = { opened++ }) { _, _, _ -> }
            }
        }

        compose.onNodeWithTag(HomeTags.TITLE).performClick()
        assertEquals("Un toque no hace nada", 0, opened)
        compose.onNodeWithTag(HomeTags.TITLE).performTouchInput { longClick() }
        assertEquals(1, opened)
    }

    @Test
    fun `el tile, el atajo y la pantalla tienen nombre neutro, sin Lente Rosa`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val label = context.getString(R.string.discreet_label)
        assertEquals("Atajo", label)
        assertFalse(label.contains("Lente", ignoreCase = true))

        val pm = context.packageManager
        val tile = pm.getServiceInfo(ComponentName(context, DiscreetTileService::class.java), PackageManager.GET_META_DATA)
        assertEquals(R.string.discreet_label, tile.labelRes)
        assertEquals(R.drawable.ic_shortcut_neutral, tile.icon)

        val activity = pm.getActivityInfo(ComponentName(context, DiscreetActivity::class.java), 0)
        assertEquals(R.string.discreet_label, activity.labelRes)
        assertTrue("Fuera de Recientes", activity.flags and android.content.pm.ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
    }

    @Test
    fun `el atajo del ícono apunta a la pantalla discreta`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val parser = context.resources.getXml(R.xml.shortcuts)
        var target: String? = null
        while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "intent") {
                target = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "targetClass")
            }
        }
        assertEquals(DiscreetActivity::class.java.name, target)
        assertEquals(DiscreetActivity::class.java.name, DiscreetActivity.intent(context).component?.className)
    }

    @Test
    fun `los grises del Modo discreto cumplen AA en claro y oscuro`() {
        listOf(DiscreetColors.Light, DiscreetColors.Dark).forEach { c ->
            assertTrue(contrast(c.text, c.background) >= 4.5)
            assertTrue(contrast(c.text, c.button) >= 4.5)
            assertTrue(contrast(c.secondaryText, c.button) >= 4.5)
            assertTrue(contrast(c.secondaryText, c.background) >= 4.5)
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        fun lum(c: Color): Double {
            fun ch(v: Float) = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
            return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
        }
        val la = lum(a)
        val lb = lum(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
