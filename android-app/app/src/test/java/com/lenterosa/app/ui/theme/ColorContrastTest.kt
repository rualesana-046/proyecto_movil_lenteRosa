package com.lenterosa.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.lenterosa.app.ui.comfort.ComfortLevel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Contraste WCAG 2.x de los tokens del sistema de diseño (RNF-02): texto normal con 4,5:1 o más.
class ColorContrastTest {

    @Test
    fun `los pares de texto y fondo del tema claro cumplen AA`() {
        with(LightTokens) {
            assertAA(onPrimary, primary, "onPrimary/primary")
            assertAA(onPrimaryContainer, primaryContainer, "onPrimaryContainer/primaryContainer")
            assertAA(onSecondary, secondary, "onSecondary/secondary")
            assertAA(onSecondaryContainer, secondaryContainer, "onSecondaryContainer/secondaryContainer")
            assertAA(onBackground, background, "onBackground/background")
            assertAA(onBackground, surfaceContainer, "onBackground/surfaceContainer")
            assertAA(onSurfaceVariant, surfaceVariant, "onSurfaceVariant/surfaceVariant")
            assertAA(onSurfaceVariant, background, "onSurfaceVariant/background")
            assertAA(primary, background, "primary/background")
            assertAA(onErrorContainer, errorContainer, "onErrorContainer/errorContainer")
        }
    }

    @Test
    fun `los pares de texto y fondo del tema oscuro cumplen AA`() {
        with(DarkTokens) {
            assertAA(onPrimary, primary, "onPrimary/primary")
            assertAA(onPrimaryContainer, primaryContainer, "onPrimaryContainer/primaryContainer")
            assertAA(onSecondary, secondary, "onSecondary/secondary")
            assertAA(onSecondaryContainer, secondaryContainer, "onSecondaryContainer/secondaryContainer")
            assertAA(onBackground, background, "onBackground/background")
            assertAA(onBackground, surfaceContainer, "onBackground/surfaceContainer")
            assertAA(onSurfaceVariant, surfaceVariant, "onSurfaceVariant/surfaceVariant")
            assertAA(onSurfaceVariant, background, "onSurfaceVariant/background")
            assertAA(primary, background, "primary/background")
            assertAA(onErrorContainer, errorContainer, "onErrorContainer/errorContainer")
        }
    }

    @Test
    fun `la escala de confort cumple AA sobre su contenedor y sobre el fondo en ambos temas`() {
        for ((palette, background) in listOf(ComfortPalette.light to LightTokens.background, ComfortPalette.dark to DarkTokens.background)) {
            assertEquals(ComfortTone.entries.toSet(), palette.keys)
            palette.forEach { (tone, colors) ->
                assertAA(colors.content, colors.container, "$tone sobre su contenedor")
                assertAA(colors.content, background, "$tone sobre el fondo")
            }
        }
    }

    @Test
    fun `la escala de confort no usa rojo`() {
        (ComfortPalette.light.values + ComfortPalette.dark.values).forEach { colors ->
            listOf(colors.content, colors.container).forEach { color ->
                val hue = hue(color)
                assertTrue("Tono rojo ($hue°) en $color", hue == null || hue in 25.0..335.0)
            }
        }
    }

    // Tono en grados (0–360), o nulo si el color es gris.
    private fun hue(color: Color): Double? {
        val r = color.red.toDouble()
        val g = color.green.toDouble()
        val b = color.blue.toDouble()
        val maxC = maxOf(r, g, b)
        val delta = maxC - minOf(r, g, b)
        if (delta < 0.02) return null
        val h = when (maxC) {
            r -> 60 * (((g - b) / delta).mod(6.0))
            g -> 60 * ((b - r) / delta + 2)
            else -> 60 * ((r - g) / delta + 4)
        }
        return h
    }

    @Test
    fun `cada etiqueta del índice tiene su propio tono, texto e ícono`() {
        assertEquals(ComfortLevel.entries.size, ComfortLevel.entries.map { it.tone }.toSet().size)
        assertEquals(ComfortLevel.entries.size, ComfortLevel.entries.map { it.label }.toSet().size)
        assertEquals(ComfortLevel.entries.size, ComfortLevel.entries.map { it.icon }.toSet().size)
    }

    @Test
    fun `un código desconocido o nulo se muestra como pocas opiniones`() {
        assertEquals(ComfortLevel.COMFORTABLE, ComfortLevel.fromCode("COMFORTABLE"))
        assertEquals(ComfortLevel.FEW_OPINIONS, ComfortLevel.fromCode("NUEVA_ETIQUETA"))
        assertEquals(ComfortLevel.FEW_OPINIONS, ComfortLevel.fromCode(null))
    }

    private fun assertAA(foreground: Color, background: Color, pair: String) {
        val ratio = contrast(foreground, background)
        assertTrue("$pair tiene contraste ${"%.2f".format(ratio)}:1, menor que 4,5:1", ratio >= 4.5)
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
