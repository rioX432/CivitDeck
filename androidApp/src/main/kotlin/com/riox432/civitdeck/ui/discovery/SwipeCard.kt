package com.riox432.civitdeck.ui.discovery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.animation.core.Spring as SpringSpec

enum class SwipeDirection { Left, Right, Up }

private const val SWIPE_THRESHOLD_FRACTION = 0.3f
private const val UP_SWIPE_THRESHOLD_FRACTION = 0.25f
private const val MAX_ROTATION_DEGREES = 15f

/**
 * Result of a committed swipe, containing the direction and the drag offset
 * at the moment the user released, so the exit animation can continue from there.
 */
data class SwipeResult(
    val direction: SwipeDirection,
    val releaseOffset: Offset,
)

@Composable
fun SwipeCard(
    modifier: Modifier = Modifier,
    onSwiped: (SwipeResult) -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val swipeThresholdPx = screenWidthPx * SWIPE_THRESHOLD_FRACTION
    val upThresholdPx = screenHeightPx * UP_SWIPE_THRESHOLD_FRACTION
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    Box(
        modifier = modifier
            .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
            .graphicsLayer {
                rotationZ = (offset.value.x / screenWidthPx) * MAX_ROTATION_DEGREES
            }
            .swipeGesture(scope, offset, swipeThresholdPx, upThresholdPx, onSwiped),
    ) {
        content()
    }
}

@Suppress("LongParameterList")
private fun Modifier.swipeGesture(
    scope: CoroutineScope,
    offset: Animatable<Offset, *>,
    swipeThreshold: Float,
    upThreshold: Float,
    onSwiped: (SwipeResult) -> Unit,
): Modifier = pointerInput(Unit) {
    detectDragGestures(
        onDragEnd = {
            val current = offset.value
            val direction = resolveDirection(current, swipeThreshold, upThreshold)
            if (direction != null) {
                // Fire immediately — don't wait for animation
                onSwiped(SwipeResult(direction, current))
            } else {
                scope.launch { offset.animateTo(Offset.Zero, returnSpring()) }
            }
        },
        onDragCancel = {
            scope.launch { offset.animateTo(Offset.Zero, returnSpring()) }
        },
    ) { change, dragAmount ->
        change.consume()
        scope.launch {
            offset.snapTo(Offset(offset.value.x + dragAmount.x, offset.value.y + dragAmount.y))
        }
    }
}

private fun resolveDirection(
    offset: Offset,
    swipeThreshold: Float,
    upThreshold: Float,
): SwipeDirection? = when {
    offset.y < -upThreshold -> SwipeDirection.Up
    offset.x > swipeThreshold -> SwipeDirection.Right
    offset.x < -swipeThreshold -> SwipeDirection.Left
    else -> null
}

private fun returnSpring() = spring<Offset>(
    dampingRatio = SpringSpec.DampingRatioMediumBouncy,
    stiffness = SpringSpec.StiffnessMedium,
)
