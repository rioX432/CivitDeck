package com.riox432.civitdeck.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.Parallax
import com.riox432.civitdeck.ui.theme.Stagger
import androidx.compose.animation.core.Spring as SpringSpec

/**
 * Returns true when the system-level animator duration scale is 0 (reduced motion).
 * Platform-specific: Android checks Settings.Global.ANIMATOR_DURATION_SCALE.
 * Other platforms return false (animations always enabled).
 */
@Composable
expect fun isReducedMotionEnabled(): Boolean

/**
 * Modifier that applies a staggered fade-in + slide-up entrance animation.
 * The [animatable] drives both alpha and vertical offset.
 *
 * When reduced motion is enabled, items appear instantly without animation.
 */
fun Modifier.staggeredEntrance(
    animatable: Animatable<Float, AnimationVector1D>,
    reducedMotion: Boolean,
): Modifier = graphicsLayer {
    if (reducedMotion) return@graphicsLayer
    alpha = animatable.value
    translationY = (1f - animatable.value) * Stagger.initialOffsetY
}

/**
 * Launches the stagger entrance animation for a single item.
 */
@Composable
fun LaunchStaggerAnimation(
    index: Int,
    animatable: Animatable<Float, AnimationVector1D>,
    reducedMotion: Boolean,
) {
    LaunchedEffect(Unit) {
        if (reducedMotion) {
            animatable.snapTo(1f)
            return@LaunchedEffect
        }
        val delay = (index * Stagger.delayPerItemMs).coerceAtMost(Stagger.maxDelayMs)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = Duration.normal,
                delayMillis = delay,
                easing = Easing.decelerate,
            ),
        )
    }
}

/**
 * Modifier that applies a parallax vertical offset to an image based on
 * scroll position.
 *
 * The parallax factor determines how much the image shifts relative to scroll.
 */
fun Modifier.parallaxEffect(
    scrollOffset: Float,
    reducedMotion: Boolean,
): Modifier = graphicsLayer {
    if (reducedMotion) return@graphicsLayer
    val offset = scrollOffset * Parallax.factor
    val clampedOffset = offset.coerceIn(-Parallax.maxOffsetDp, Parallax.maxOffsetDp)
    translationY = clampedOffset
}

/**
 * Modifier that applies a spring-based press scale effect for card interactions.
 */
fun Modifier.springScale(
    pressed: Boolean,
    reducedMotion: Boolean,
): Modifier = composed {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(pressed) {
        if (reducedMotion) return@LaunchedEffect
        scale.animateTo(
            targetValue = if (pressed) 0.96f else 1f,
            animationSpec = spring(
                dampingRatio = SpringSpec.DampingRatioMediumBouncy,
                stiffness = SpringSpec.StiffnessMedium,
            ),
        )
    }

    graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}
