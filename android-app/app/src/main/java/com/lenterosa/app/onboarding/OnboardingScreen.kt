package com.lenterosa.app.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lenterosa.app.R
import com.lenterosa.app.ui.glass.currentMotion
import com.lenterosa.app.ui.glass.glassSurface
import kotlinx.coroutines.launch

/** Los tres pasos de la bienvenida (RF-11). */
enum class OnboardingStep(@param:DrawableRes val icon: Int, @param:StringRes val title: Int, @param:StringRes val body: Int) {
    PERCEPTION(R.drawable.ic_comfort_mixed, R.string.onboarding_perception_title, R.string.onboarding_perception_body),
    PRIVACY(R.drawable.ic_privacy, R.string.onboarding_privacy_title, R.string.onboarding_privacy_body),
    CONTRIBUTE(R.drawable.ic_account, R.string.onboarding_contribute_title, R.string.onboarding_contribute_body),
}

object OnboardingTags {
    const val NEXT = "onboarding_next"
    const val SKIP = "onboarding_skip"
    const val PROGRESS = "onboarding_progress"
}

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel, onFinished: () -> Unit) {
    OnboardingScreen(onFinish = { viewModel.finish(onFinished) })
}

/** Bienvenida al primer uso: tres pasos que se pueden deslizar o saltar. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val steps = OnboardingStep.entries
    val pager = rememberPagerState { steps.size }
    val scope = rememberCoroutineScope()
    val motion = currentMotion()
    val isLast = pager.currentPage == steps.lastIndex
    val colors = MaterialTheme.colorScheme

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.background, colors.primaryContainer.copy(alpha = 0.45f)))),
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End) {
                if (!isLast) {
                    TextButton(onClick = onFinish, modifier = Modifier.testTag(OnboardingTags.SKIP)) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page -> StepPage(steps[page]) }
            Progress(current = pager.currentPage, total = steps.size)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (isLast) {
                        onFinish()
                    } else {
                        scope.launch {
                            val next = pager.currentPage + 1
                            if (motion.reduced) pager.scrollToPage(next) else pager.animateScrollToPage(next)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag(OnboardingTags.NEXT),
            ) {
                Text(stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next))
            }
        }
    }
}

@Composable
private fun StepPage(step: OnboardingStep) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(128.dp).glassSurface(null, RoundedCornerShape(64.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(step.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp))
        }
        Text(
            text = stringResource(step.title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(step.body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Avance con texto ("Paso 2 de 3") y puntos; el punto actual es más ancho, no solo de otro color. */
@Composable
private fun Progress(current: Int, total: Int) {
    val description = stringResource(R.string.onboarding_progress, current + 1, total)
    val motion = currentMotion()
    Column(
        Modifier.fillMaxWidth().testTag(OnboardingTags.PROGRESS).clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(total) { index ->
                val active = index == current
                Box(
                    Modifier
                        .animateContentSize(motion.spec())
                        .height(8.dp)
                        .width(if (active) 24.dp else 8.dp)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                )
            }
        }
        Text(description, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
