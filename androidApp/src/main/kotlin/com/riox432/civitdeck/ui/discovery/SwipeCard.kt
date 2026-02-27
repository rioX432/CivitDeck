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

@Composable
fun SwipeCard(
    modifier: Modifier = Modifier,
    onSwiped: (SwipeDirection) -> Unit,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val swipeThresholdPx = screenWidth.value * SWIPE_THRESHOLD_FRACTION
    val upThresholdPx = screenHeight.value * UP_SWIPE_THRESHOLD_FRACTION
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    Box(
        modifier = modifier
            .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
            .graphicsLayer {
                rotationZ = (offset.value.x / screenWidth.value) * MAX_ROTATION_DEGREES
            }
            .swipeGesture(
                scope,
                offset,
                swipeThresholdPx,
                upThresholdPx,
                screenWidth.value,
                screenHeight.value,
                onSwiped
            ),
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
    screenWidth: Float,
    screenHeight: Float,
    onSwiped: (SwipeDirection) -> Unit,
): Modifier = pointerInput(Unit) {
    detectDragGestures(
        onDragEnd = {
            scope.launch {
                resolveSwipe(offset, swipeThreshold, upThreshold, screenWidth, screenHeight, onSwiped)
            }
        },
        onDragCancel = {
            scope.launch { offset.animateTo(Offset.Zero, animationSpec = returnSpring()) }
        },
    ) { change, dragAmount ->
        change.consume()
        scope.launch {
            offset.snapTo(Offset(offset.value.x + dragAmount.x, offset.value.y + dragAmount.y))
        }
    }
}

@Suppress("LongParameterList")
private suspend fun resolveSwipe(
    offset: Animatable<Offset, *>,
    swipeThreshold: Float,
    upThreshold: Float,
    screenWidth: Float,
    screenHeight: Float,
    onSwiped: (SwipeDirection) -> Unit,
) {
    val current = offset.value
    when {
        current.y < -upThreshold -> {
            offset.animateTo(Offset(current.x, -screenHeight * 2), exitSpring())
            onSwiped(SwipeDirection.Up)
        }
        current.x > swipeThreshold -> {
            offset.animateTo(Offset(screenWidth * 2, current.y), exitSpring())
            onSwiped(SwipeDirection.Right)
        }
        current.x < -swipeThreshold -> {
            offset.animateTo(Offset(-screenWidth * 2, current.y), exitSpring())
            onSwiped(SwipeDirection.Left)
        }
        else -> offset.animateTo(Offset.Zero, returnSpring())
    }
}

private fun exitSpring() = spring<Offset>(
    dampingRatio = SpringSpec.DampingRatioNoBouncy,
    stiffness = SpringSpec.StiffnessLow,
)

private fun returnSpring() = spring<Offset>(
    dampingRatio = SpringSpec.DampingRatioMediumBouncy,
    stiffness = SpringSpec.StiffnessMedium,
)
