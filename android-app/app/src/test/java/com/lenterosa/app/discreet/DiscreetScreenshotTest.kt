package com.lenterosa.app.discreet

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.lenterosa.app.ui.glass.ProvideVisualPreferences
import com.lenterosa.app.ui.glass.VisualPreferences
import com.lenterosa.app.ui.theme.LenteRosaTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Dibuja las pantallas del Modo discreto en claro y oscuro y las guarda en build/capturas. En el
 * teléfono no se pueden capturar: la pantalla bloquea las capturas a propósito (FLAG_SECURE).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class DiscreetScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val settings = DiscreetSettings(contacts = listOf(TrustedContact("Sofía", "3001234567")), fakeCallName = "Mamá", fakeCallDelaySeconds = 10)

    @Test
    fun pantallaPrincipalClaro() = capture("discreto-principal-claro", dark = false) { colors ->
        DiscreetScreen(DiscreetUiState(settings = settings), colors, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun pantallaPrincipalOscuro() = capture("discreto-principal-oscuro", dark = true) { colors ->
        DiscreetScreen(DiscreetUiState(settings = settings), colors, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test
    fun deshacerYEstadoClaro() = capture("discreto-deshacer-claro", dark = false) { colors ->
        DiscreetScreen(
            DiscreetUiState(settings = settings, call = FakeCall.Scheduled(8), undo = UndoOffer.Call(10),
                sendStatus = SendStatus.MESSAGE_READY_APPROXIMATE),
            colors, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }

    @Test
    fun deshacerYEstadoOscuro() = capture("discreto-deshacer-oscuro", dark = true) { colors ->
        DiscreetScreen(
            DiscreetUiState(settings = settings, undo = UndoOffer.Mark("m1", withLocation = true), sendStatus = SendStatus.MESSAGE_READY),
            colors, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }

    @Test
    fun llamadaSimulada() = capture("discreto-llamada", dark = true) { _ -> FakeCallScreen(FakeCall.Ringing("Mamá"), {}, {}) }

    @Test
    fun ajustesClaro() = capture("discreto-ajustes-claro", dark = false) { colors ->
        DiscreetSettingsScreen(settings, colors, { _, _ -> }, { _, _ -> }, {}, {})
    }

    @Test
    fun ajustesOscuro() = capture("discreto-ajustes-oscuro", dark = true) { colors ->
        DiscreetSettingsScreen(settings, colors, { _, _ -> }, { _, _ -> }, {}, {})
    }

    private fun capture(name: String, dark: Boolean, content: @Composable (DiscreetColors) -> Unit) {
        compose.setContent {
            LenteRosaTheme(darkTheme = dark) {
                ProvideVisualPreferences(VisualPreferences(discreetMode = true)) {
                    content(if (dark) DiscreetColors.Dark else DiscreetColors.Light)
                }
            }
        }
        val folder = File("build/capturas").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
