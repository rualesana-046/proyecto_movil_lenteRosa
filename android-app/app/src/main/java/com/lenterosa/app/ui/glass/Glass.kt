package com.lenterosa.app.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassAccessibilitySettings
import dev.chrisbanes.haze.glass.GlassReducedMotionPolicy
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.LocalGlassAccessibilitySettings
import dev.chrisbanes.haze.glass.hazeGlass

/** Cómo se dibujan las superficies de vidrio (RNF-03, ADR 0010). */
enum class GlassMode {
    /** Liquid glass: desenfoque y refracción del contenido de atrás, con un tinte que asegura el contraste. */
    GLASS,

    /** Superficie opaca del tema, sin nada del contenido de atrás. */
    OPAQUE,
}

/** Decide el modo de vidrio. El Modo discreto (RF-08) nunca usa liquid glass. */
object GlassPolicy {
    fun resolve(discreetMode: Boolean, reduceTransparency: Boolean): GlassMode =
        if (discreetMode || reduceTransparency) GlassMode.OPAQUE else GlassMode.GLASS
}

/**
 * Tinte del vidrio sobre el contenido desenfocado. Con esta opacidad, el texto del tema cumple
 * WCAG AA sobre cualquier fondo, del negro al blanco (GlassContrastTest lo verifica).
 */
object GlassTokens {
    const val TINT_ALPHA = 0.9f

    fun tint(surface: Color): Color = surface.copy(alpha = TINT_ALPHA)

    /** Peor caso del vidrio: el tinte compuesto sobre un fondo dado (en las pruebas, negro y blanco). */
    fun compositeOver(surface: Color, backdrop: Color): Color = tint(surface).compositeOver(backdrop)
}

/** Preferencias del sistema y del modo de la app que afectan al vidrio y al movimiento. */
data class VisualPreferences(
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = false,
    val discreetMode: Boolean = false,
) {
    val glassMode: GlassMode get() = GlassPolicy.resolve(discreetMode, reduceTransparency = highContrast)
}

val LocalVisualPreferences = staticCompositionLocalOf { VisualPreferences() }

/** Aplica las preferencias a todo el árbol, incluida la configuración de accesibilidad de Haze. */
@Composable
fun ProvideVisualPreferences(preferences: VisualPreferences, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalVisualPreferences provides preferences,
        LocalGlassAccessibilitySettings provides GlassAccessibilitySettings(
            reduceTransparency = preferences.highContrast,
            increaseContrast = preferences.highContrast,
            showBorders = preferences.highContrast,
        ),
        content = content,
    )
}

/** Marca semántica de las superficies de vidrio, para comprobar en pruebas que no tapan la atribución. */
val GlassSurfaceKey = SemanticsPropertyKey<Boolean>("GlassSurface")
private var SemanticsPropertyReceiver.glassSurface by GlassSurfaceKey

object LenteRosaGlass {
    @Composable
    @ReadOnlyComposable
    fun mode(): GlassMode = LocalVisualPreferences.current.glassMode
}

/**
 * Superficie de vidrio con la forma dada. `state` es el HazeState del contenido que queda detrás.
 * Sin `state` (por ejemplo sobre el mapa, cuya vista nativa no se puede capturar para desenfocar)
 * se dibuja el mismo tinte translúcido sin desenfoque, con el mismo contraste garantizado.
 * En modo opaco (Modo discreto o alto contraste) dibuja el color de superficie del tema.
 *
 * El tinte se dibuja siempre debajo del efecto: si el dispositivo no puede renderizar el vidrio
 * (Haze no dibuja nada en ese caso), el contraste sigue garantizado.
 */
@Composable
fun Modifier.glassSurface(state: HazeState?, shape: RoundedCornerShape): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    val base = semantics { glassSurface = true }
    return when {
        LenteRosaGlass.mode() == GlassMode.OPAQUE -> this.then(base).background(MaterialTheme.colorScheme.surfaceContainer, shape)
        state == null -> this.then(base).background(GlassTokens.tint(surface), shape)
        else -> this.then(base).background(GlassTokens.tint(surface), shape).hazeGlass(
            input = HazeInput.Sources(state),
            style = GlassStyle.regular.then {
                shape(shape)
                tint(GlassTokens.tint(surface))
            },
            interactionReducedMotionPolicy = if (LocalVisualPreferences.current.reducedMotion) {
                GlassReducedMotionPolicy.Reduced
            } else {
                GlassReducedMotionPolicy.System
            },
        )
    }
}
