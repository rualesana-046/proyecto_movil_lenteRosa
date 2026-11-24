package com.lenterosa.app.saved

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.lenterosa.app.places.Comfort
import com.lenterosa.app.places.DimensionAverages
import com.lenterosa.app.places.Location
import com.lenterosa.app.places.PlaceDetail
import com.lenterosa.app.ui.theme.LenteRosaTheme
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Dibuja la pestaña Guardados con datos de ejemplo, en claro y oscuro, y guarda la imagen en
 * build/capturas. Sirve para revisar los avisos sin iniciar sesión en un dispositivo. También
 * comprueba que los textos clave aparecen.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = android.app.Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class SavedScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private val parque = PlaceDetail(
        id = "p1", name = "[FICTICIO] Parque de Prueba Norte", category = "PARK", location = Location(1.22, -77.279),
        reviewCount = 5, lastReviewAt = "2026-11-21T20:00:00Z", averages = DimensionAverages(3.0, 3.0, 2.8, 2.7),
        comfort = Comfort(index = 52, label = "MIXED_OPINIONS", uncertainty = "MEDIUM", nightDataMissing = false),
    )
    private val tienda = parque.copy(
        id = "p2", name = "[FICTICIO] Tienda de Ejemplo Centro", category = "COMMERCE",
        comfort = Comfort(index = 75, label = "VERY_COMFORTABLE", uncertainty = "LOW", nightDataMissing = false),
    )
    private val state = SavedLoad.Ready(
        SavedOverview(
            places = listOf(parque, tienda),
            inbox = AlertInbox(
                unreadCount = 1,
                items = listOf(
                    AlertDto("a1", "p1", "COMFORTABLE", 66, "MIXED_OPINIONS", 52, "2026-11-21T20:00:00Z", read = false),
                    AlertDto("a2", "p2", "COMFORTABLE", 68, "VERY_COMFORTABLE", 75, "2026-11-10T15:00:00Z", read = true),
                ),
            ),
        ),
    )

    @Test
    fun `pestaña Guardados con avisos en modo claro`() = capture(dark = false, name = "guardados-claro")

    @Test
    fun `pestaña Guardados con avisos en modo oscuro`() = capture(dark = true, name = "guardados-oscuro")

    private fun capture(dark: Boolean, name: String) {
        compose.setContent {
            LenteRosaTheme(darkTheme = dark) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SavedList(state, onOpenPlace = {}, onOpenAccount = {}, onRetry = {}, onRemove = {})
                }
            }
        }
        compose.onNodeWithText("Cambió cómo lo percibe la comunidad: ahora «Opiniones mixtas», antes «Cómodo».").assertExists()
        compose.onNodeWithText("Nuevo").assertExists()
        val folder = File("build/capturas").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
