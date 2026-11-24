package com.lenterosa.app.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
 * Capas del vidrio de Lente Rosa (RNF-03, ADR 0010), de abajo hacia arriba:
 * 1. desenfoque con saturación del contenido de atrás (Haze, cuando el dispositivo lo soporta);
 * 2. degradado translúcido: tinte del color de superficie, más denso arriba y con un toque rosa abajo;
 * 3. resplandor interno: luz suave que nace en la esquina superior;
 * 4. brillo de borde: un filo de luz más intenso arriba.
 *
 * El degradado garantiza WCAG AA sobre cualquier fondo, del negro al blanco, incluso con el
 * resplandor en su punto máximo (GlassContrastTest lo verifica en ambos temas).
 */
object GlassTokens {
    /** Opacidad mínima del degradado (abajo). Arriba es más denso. */
    const val TINT_ALPHA = 0.9f
    const val TINT_TOP_ALPHA = 0.95f

    /** Cuánto rosa principal se mezcla en la parte baja del degradado. */
    const val ROSE_MIX = 0.07f

    /** Saturación del contenido desenfocado (1 = sin cambio). */
    const val SATURATION = 1.4f

    fun tint(surface: Color): Color = surface.copy(alpha = TINT_ALPHA)

    fun gradientTop(surface: Color): Color = surface.copy(alpha = TINT_TOP_ALPHA)

    fun gradientBottom(surface: Color, primary: Color): Color = lerp(surface, primary, ROSE_MIX).copy(alpha = TINT_ALPHA)

    /** Color máximo del resplandor interno: blanco en claro, rosa tenue en oscuro (no aclara el texto claro). */
    fun glow(dark: Boolean, primary: Color): Color = if (dark) primary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.35f)

    fun rimTop(dark: Boolean): Color = Color.White.copy(alpha = if (dark) 0.3f else 0.75f)

    fun rimBottom(dark: Boolean): Color = Color.White.copy(alpha = if (dark) 0.06f else 0.2f)

    /** Peor caso del vidrio: el tinte compuesto sobre un fondo dado (en las pruebas, negro y blanco). */
    fun compositeOver(surface: Color, backdrop: Color): Color = tint(surface).compositeOver(backdrop)

    /** Colores de fondo posibles bajo el texto: cada extremo del degradado, con y sin resplandor. */
    fun backgroundsUnderText(surface: Color, primary: Color, dark: Boolean, backdrop: Color): List<Color> {
        val stops = listOf(gradientTop(surface), gradientBottom(surface, primary)).map { it.compositeOver(backdrop) }
        return stops + stops.map { glow(dark, primary).compositeOver(it) }
    }
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
 * El degradado se dibuja siempre, encima del desenfoque: si el dispositivo no puede renderizar
 * el vidrio (Haze no dibuja nada en ese caso), el contraste sigue garantizado.
 */
@Composable
fun Modifier.glassSurface(state: HazeState?, shape: RoundedCornerShape): Modifier {
    val colors = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val base = this.then(Modifier.semantics { glassSurface = true })
    if (LenteRosaGlass.mode() == GlassMode.OPAQUE) {
        // Modo discreto o alto contraste: superficie opaca, sin ningún ingrediente del vidrio.
        return base.background(colors.surfaceContainer, shape)
    }
    val blurred = if (state == null) {
        base
    } else {
        base.hazeGlass(
            input = HazeInput.Sources(state),
            style = GlassStyle.regular.then {
                shape(shape)
                chromaMultiplier(GlassTokens.SATURATION)
            },
            interactionReducedMotionPolicy = if (LocalVisualPreferences.current.reducedMotion) {
                GlassReducedMotionPolicy.Reduced
            } else {
                GlassReducedMotionPolicy.System
            },
        )
    }
    // El degradado va encima del desenfoque: si el dispositivo no puede dibujar el vidrio,
    // el degradado sigue ahí y el contraste no cambia.
    return blurred
        .background(Brush.verticalGradient(listOf(GlassTokens.gradientTop(colors.surface), GlassTokens.gradientBottom(colors.surface, colors.primary))), shape)
        .clip(shape)
        .drawBehind {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(GlassTokens.glow(dark, colors.primary), Color.Transparent),
                    center = Offset(size.width * 0.2f, 0f),
                    radius = maxOf(size.width, size.height) * 0.7f,
                ),
            )
        }
        .border(1.dp, Brush.verticalGradient(listOf(GlassTokens.rimTop(dark), GlassTokens.rimBottom(dark))), shape)
}
