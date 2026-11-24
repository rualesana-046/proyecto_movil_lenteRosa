package com.lenterosa.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalComfortPalette = staticCompositionLocalOf { ComfortPalette.light }

// Tema de Lente Rosa en claro y oscuro: tokens rosa y ciruela, Plus Jakarta Sans y la escala de confort.
// Sin colores dinámicos del fondo de pantalla, para que el contraste verificado se mantenga (RNF-02).
@Composable
fun LenteRosaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalComfortPalette provides if (darkTheme) ComfortPalette.dark else ComfortPalette.light) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = LenteRosaTypography,
            content = content,
        )
    }
}

/** Colores de la escala de confort del tema actual. */
object LenteRosaComfort {
    @Composable
    @ReadOnlyComposable
    fun colors(tone: ComfortTone): ComfortColors = LocalComfortPalette.current.getValue(tone)
}
