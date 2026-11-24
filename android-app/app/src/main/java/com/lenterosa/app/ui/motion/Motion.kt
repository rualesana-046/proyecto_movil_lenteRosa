package com.lenterosa.app.ui.motion

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

/**
 * Movimiento de la app (RNF-03, ADR 0010): transiciones cortas y suaves. Con "Quitar animaciones"
 * activado en el sistema, todo cambia sin animación (RNF-02).
 */
class Motion(val reduced: Boolean) {

    fun <T> spec(durationMillis: Int = STANDARD_MS): FiniteAnimationSpec<T> =
        if (reduced) snap() else tween(durationMillis, easing = EMPHASIZED_DECELERATE)

    private fun offsetSpec(durationMillis: Int = STANDARD_MS): FiniteAnimationSpec<IntOffset> =
        if (reduced) snap() else tween(durationMillis, easing = EMPHASIZED_DECELERATE)

    /** Tarjetas y avisos que aparecen desde abajo. */
    fun riseIn(): EnterTransition =
        if (reduced) EnterTransition.None else slideInVertically(offsetSpec()) { it / 3 } + fadeIn(spec())

    fun sinkOut(): ExitTransition =
        if (reduced) ExitTransition.None else slideOutVertically(offsetSpec(SHORT_MS)) { it / 3 } + fadeOut(spec(SHORT_MS))

    /** Botones que aparecen en su lugar. */
    fun popIn(): EnterTransition = if (reduced) EnterTransition.None else scaleIn(spec(), initialScale = 0.9f) + fadeIn(spec())

    fun popOut(): ExitTransition = if (reduced) ExitTransition.None else scaleOut(spec(SHORT_MS), targetScale = 0.9f) + fadeOut(spec(SHORT_MS))

    /** Cambio de pantalla: hacia adelante entra desde el final; hacia atrás, desde el inicio. */
    fun <S> screenTransition(forward: Boolean): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        if (reduced) {
            EnterTransition.None togetherWith ExitTransition.None
        } else {
            val direction = if (forward) 1 else -1
            (slideInHorizontally(offsetSpec()) { direction * it / 5 } + fadeIn(spec())) togetherWith
                (slideOutHorizontally(offsetSpec(SHORT_MS)) { -direction * it / 8 } + fadeOut(spec(SHORT_MS)))
        }
    }

    /** Cambio entre pestañas: solo un fundido. */
    fun <S> tabTransition(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
        if (reduced) EnterTransition.None togetherWith ExitTransition.None else fadeIn(spec()) togetherWith fadeOut(spec(SHORT_MS))
    }

    companion object {
        const val STANDARD_MS = 280
        const val SHORT_MS = 180
        private val EMPHASIZED_DECELERATE = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

        /** "Quitar animaciones" del sistema deja la escala de animación en 0. */
        fun systemReducedMotion(context: Context): Boolean {
            val resolver = context.contentResolver
            val animator = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            val transition = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
            return animator == 0f || transition == 0f
        }

        /** "Texto de alto contraste" de Accesibilidad: el vidrio pasa a superficies opacas. */
        fun systemHighContrastText(context: Context): Boolean =
            Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0) == 1
    }
}
