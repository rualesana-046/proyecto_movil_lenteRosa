package com.lenterosa.app.ui.glass

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.ui.graphics.Color
import com.lenterosa.app.ui.motion.Motion
import com.lenterosa.app.ui.theme.DarkTokens
import com.lenterosa.app.ui.theme.LightTokens
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El vidrio deja ver el contenido de atrás, que puede ser de cualquier color. El tinte debe
 * garantizar WCAG AA (4,5:1) para el texto del tema en el peor caso: fondo negro o blanco.
 * Como la luminancia crece con cada canal, cualquier otro fondo queda entre esos dos extremos.
 */
class GlassContrastTest {

    private val backdrops = listOf(Color.Black, Color.White)

    @Test
    fun `el texto sobre vidrio cumple AA con cualquier fondo en el tema claro`() {
        with(LightTokens) {
            assertTextOnGlass(onBackground, background, "onBackground")
            assertTextOnGlass(onSurfaceVariant, background, "onSurfaceVariant")
            assertTextOnGlass(primary, background, "primary")
        }
    }

    @Test
    fun `el texto sobre vidrio cumple AA con cualquier fondo en el tema oscuro`() {
        with(DarkTokens) {
            assertTextOnGlass(onBackground, background, "onBackground")
            assertTextOnGlass(onSurfaceVariant, background, "onSurfaceVariant")
            assertTextOnGlass(primary, background, "primary")
        }
    }

    @Test
    fun `el Modo discreto nunca usa liquid glass`() {
        assertEquals(GlassMode.OPAQUE, GlassPolicy.resolve(discreetMode = true, reduceTransparency = false))
        assertEquals(GlassMode.OPAQUE, GlassPolicy.resolve(discreetMode = true, reduceTransparency = true))
        assertEquals(GlassMode.OPAQUE, VisualPreferences(discreetMode = true).glassMode)
    }

    @Test
    fun `con texto de alto contraste el vidrio pasa a superficie opaca`() {
        assertEquals(GlassMode.OPAQUE, VisualPreferences(highContrast = true).glassMode)
        assertEquals(GlassMode.GLASS, VisualPreferences().glassMode)
    }

    @Test
    fun `con Quitar animaciones no hay transiciones`() {
        val reduced = Motion(reduced = true)
        assertEquals(EnterTransition.None, reduced.riseIn())
        assertEquals(ExitTransition.None, reduced.sinkOut())
        assertEquals(EnterTransition.None, reduced.popIn())
        assertEquals(ExitTransition.None, reduced.popOut())

        val full = Motion(reduced = false)
        assertNotEquals(EnterTransition.None, full.riseIn())
        assertNotEquals(ExitTransition.None, full.sinkOut())
    }

    private fun assertTextOnGlass(text: Color, surface: Color, name: String) {
        val sides = backdrops.map { backdrop ->
            val glass = GlassTokens.compositeOver(surface, backdrop)
            val ratio = contrast(text, glass)
            assertTrue("$name sobre vidrio con fondo $backdrop: ${"%.2f".format(ratio)}:1, menor que 4,5:1", ratio >= 4.5)
            luminance(text) < luminance(glass)
        }
        // El texto queda siempre del mismo lado (más oscuro o más claro que el vidrio): ningún fondo
        // intermedio puede igualar su luminancia.
        assertEquals("$name cruza la luminancia del vidrio entre fondos", 1, sides.toSet().size)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun channel(c: Float): Double = if (c <= 0.04045f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }
}
