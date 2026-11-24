package com.lenterosa.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Tokens de color del sistema de diseño (RNF-03): rosa como color principal y ciruela como secundario.
// Cada pareja de texto y fondo cumple WCAG AA (4,5:1 o más); ColorContrastTest lo verifica (RNF-02).

internal object LightTokens {
    val primary = Color(0xFF9C2C5F)
    val onPrimary = Color(0xFFFFFFFF)
    val primaryContainer = Color(0xFFFFD9E3)
    val onPrimaryContainer = Color(0xFF3D0020)
    val secondary = Color(0xFF6D3A64)
    val onSecondary = Color(0xFFFFFFFF)
    val secondaryContainer = Color(0xFFF7D8EE)
    val onSecondaryContainer = Color(0xFF2B0F26)
    val background = Color(0xFFFFF8F8)
    val onBackground = Color(0xFF22191C)
    val surfaceVariant = Color(0xFFF2DDE2)
    val onSurfaceVariant = Color(0xFF514347)
    val surfaceContainer = Color(0xFFF9ECEF)
    val outline = Color(0xFF83737A)
    val outlineVariant = Color(0xFFD5C2C6)
    val error = Color(0xFFBA1A1A)
    val onError = Color(0xFFFFFFFF)
    val errorContainer = Color(0xFFFFDAD6)
    val onErrorContainer = Color(0xFF410002)
}

internal object DarkTokens {
    val primary = Color(0xFFFFB1C9)
    val onPrimary = Color(0xFF5E1135)
    val primaryContainer = Color(0xFF7E2A4D)
    val onPrimaryContainer = Color(0xFFFFD9E3)
    val secondary = Color(0xFFE3B7D8)
    val onSecondary = Color(0xFF43243B)
    val secondaryContainer = Color(0xFF5B3A52)
    val onSecondaryContainer = Color(0xFFFFD7F3)
    val background = Color(0xFF191113)
    val onBackground = Color(0xFFEFDFE2)
    val surfaceVariant = Color(0xFF514347)
    val onSurfaceVariant = Color(0xFFD5C2C6)
    val surfaceContainer = Color(0xFF261D20)
    val outline = Color(0xFF9E8C91)
    val outlineVariant = Color(0xFF514347)
    val error = Color(0xFFFFB4AB)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)
}

internal val LightColors = lightColorScheme(
    primary = LightTokens.primary,
    onPrimary = LightTokens.onPrimary,
    primaryContainer = LightTokens.primaryContainer,
    onPrimaryContainer = LightTokens.onPrimaryContainer,
    secondary = LightTokens.secondary,
    onSecondary = LightTokens.onSecondary,
    secondaryContainer = LightTokens.secondaryContainer,
    onSecondaryContainer = LightTokens.onSecondaryContainer,
    tertiary = LightTokens.secondary,
    onTertiary = LightTokens.onSecondary,
    background = LightTokens.background,
    onBackground = LightTokens.onBackground,
    surface = LightTokens.background,
    onSurface = LightTokens.onBackground,
    surfaceVariant = LightTokens.surfaceVariant,
    onSurfaceVariant = LightTokens.onSurfaceVariant,
    surfaceContainer = LightTokens.surfaceContainer,
    surfaceContainerLow = LightTokens.surfaceContainer,
    surfaceContainerHigh = LightTokens.surfaceVariant,
    outline = LightTokens.outline,
    outlineVariant = LightTokens.outlineVariant,
    error = LightTokens.error,
    onError = LightTokens.onError,
    errorContainer = LightTokens.errorContainer,
    onErrorContainer = LightTokens.onErrorContainer,
)

internal val DarkColors = darkColorScheme(
    primary = DarkTokens.primary,
    onPrimary = DarkTokens.onPrimary,
    primaryContainer = DarkTokens.primaryContainer,
    onPrimaryContainer = DarkTokens.onPrimaryContainer,
    secondary = DarkTokens.secondary,
    onSecondary = DarkTokens.onSecondary,
    secondaryContainer = DarkTokens.secondaryContainer,
    onSecondaryContainer = DarkTokens.onSecondaryContainer,
    tertiary = DarkTokens.secondary,
    onTertiary = DarkTokens.onSecondary,
    background = DarkTokens.background,
    onBackground = DarkTokens.onBackground,
    surface = DarkTokens.background,
    onSurface = DarkTokens.onBackground,
    surfaceVariant = DarkTokens.surfaceVariant,
    onSurfaceVariant = DarkTokens.onSurfaceVariant,
    surfaceContainer = DarkTokens.surfaceContainer,
    surfaceContainerLow = DarkTokens.surfaceContainer,
    surfaceContainerHigh = DarkTokens.surfaceVariant,
    outline = DarkTokens.outline,
    outlineVariant = DarkTokens.outlineVariant,
    error = DarkTokens.error,
    onError = DarkTokens.onError,
    errorContainer = DarkTokens.errorContainer,
    onErrorContainer = DarkTokens.onErrorContainer,
)

/**
 * Colores de la escala del Índice de Confort. Sin rojo: el rojo sugiere peligro y el índice solo
 * refleja percepción. Siempre se acompañan de texto e ícono, nunca van solos.
 */
data class ComfortColors(val content: Color, val container: Color)

internal object ComfortPalette {
    val light = mapOf(
        ComfortTone.VERY_COMFORTABLE to ComfortColors(Color(0xFF00585A), Color(0xFFC8EEEC)),
        ComfortTone.COMFORTABLE to ComfortColors(Color(0xFF2F5F3A), Color(0xFFD3ECD2)),
        ComfortTone.MIXED to ComfortColors(Color(0xFF6B4E00), Color(0xFFF8E2B0)),
        ComfortTone.UNCOMFORTABLE to ComfortColors(Color(0xFF5B3F8F), Color(0xFFE7DBFA)),
        ComfortTone.FEW_OPINIONS to ComfortColors(Color(0xFF4F4A52), Color(0xFFE6E0E8)),
    )
    val dark = mapOf(
        ComfortTone.VERY_COMFORTABLE to ComfortColors(Color(0xFF8ED6D3), Color(0xFF00393B)),
        ComfortTone.COMFORTABLE to ComfortColors(Color(0xFFA9D3A9), Color(0xFF173A21)),
        ComfortTone.MIXED to ComfortColors(Color(0xFFEDC66A), Color(0xFF4A3500)),
        ComfortTone.UNCOMFORTABLE to ComfortColors(Color(0xFFCDB8F5), Color(0xFF3C2470)),
        ComfortTone.FEW_OPINIONS to ComfortColors(Color(0xFFCDC6CF), Color(0xFF38333A)),
    )
}

/** Tonos de la escala de confort, uno por etiqueta del índice. */
enum class ComfortTone { VERY_COMFORTABLE, COMFORTABLE, MIXED, UNCOMFORTABLE, FEW_OPINIONS }
