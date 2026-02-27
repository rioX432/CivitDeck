package com.riox432.civitdeck.ui.tutorial

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.ui.theme.Duration

private val AnimationBoxSize = 200.dp
private const val CardWidthFraction = 0.45f
private const val CardHeightFraction = 0.55f
private const val FingerRadius = 12f
private const val CardCornerRadius = 16f
private const val SwipeAmplitude = 60f
private const val DragAmplitude = 50f
private const val SliderAmplitude = 60f

@Composable
fun SwipeDiscoveryAnimation(accentColor: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "swipe")
    val offsetX by transition.animateFloat(
        initialValue = -SwipeAmplitude,
        targetValue = SwipeAmplitude,
        animationSpec = infiniteRepeatable(tween(Duration.slow * 2), RepeatMode.Reverse),
        label = "swipeX",
    )
    val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh
    AnimationCanvas(modifier) { size ->
        drawCard(size, cardColor)
        drawFinger(
            center = Offset(size.width / 2f + offsetX, size.height * 0.72f),
            color = accentColor,
        )
    }
}

@Composable
fun QuickActionsAnimation(accentColor: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "quickAction")
    val offsetX by transition.animateFloat(
        initialValue = 0f,
        targetValue = DragAmplitude,
        animationSpec = infiniteRepeatable(tween(Duration.slow * 2), RepeatMode.Reverse),
        label = "dragX",
    )
    val cardColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val revealColor = accentColor.copy(alpha = 0.3f)
    AnimationCanvas(modifier) { size ->
        drawRoundRect(
            color = revealColor,
            topLeft = Offset(size.width * 0.28f, size.height * 0.18f),
            size = Size(size.width * CardWidthFraction, size.height * CardHeightFraction),
            cornerRadius = CornerRadius(CardCornerRadius),
        )
        drawRoundRect(
            color = cardColor,
            topLeft = Offset(size.width * 0.28f + offsetX, size.height * 0.18f),
            size = Size(size.width * CardWidthFraction, size.height * CardHeightFraction),
            cornerRadius = CornerRadius(CardCornerRadius),
        )
        drawFinger(
            center = Offset(size.width * 0.5f + offsetX, size.height * 0.72f),
            color = accentColor,
        )
    }
}

@Composable
fun ImageComparisonAnimation(accentColor: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "slider")
    val sliderX by transition.animateFloat(
        initialValue = -SliderAmplitude,
        targetValue = SliderAmplitude,
        animationSpec = infiniteRepeatable(tween(Duration.slow * 2), RepeatMode.Reverse),
        label = "sliderX",
    )
    val leftColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val rightColor = MaterialTheme.colorScheme.surfaceContainerHighest
    AnimationCanvas(modifier) { size ->
        val cardLeft = size.width * 0.15f
        val cardTop = size.height * 0.15f
        val cardW = size.width * 0.7f
        val cardH = size.height * 0.6f
        val dividerX = cardLeft + cardW / 2f + sliderX
        drawRect(leftColor, Offset(cardLeft, cardTop), Size(dividerX - cardLeft, cardH))
        drawRect(rightColor, Offset(dividerX, cardTop), Size(cardLeft + cardW - dividerX, cardH))
        drawLine(accentColor, Offset(dividerX, cardTop), Offset(dividerX, cardTop + cardH), 3f)
        drawFinger(Offset(dividerX, cardTop + cardH + 20f), accentColor)
    }
}

@Composable
private fun AnimationCanvas(
    modifier: Modifier,
    onDraw: androidx.compose.ui.graphics.drawscope.DrawScope.(Size) -> Unit,
) {
    Box(modifier = modifier.size(AnimationBoxSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) { onDraw(size) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCard(
    canvasSize: Size,
    color: Color,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(canvasSize.width * 0.28f, canvasSize.height * 0.18f),
        size = Size(canvasSize.width * CardWidthFraction, canvasSize.height * CardHeightFraction),
        cornerRadius = CornerRadius(CardCornerRadius),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinger(
    center: Offset,
    color: Color,
) {
    drawCircle(color = color.copy(alpha = 0.25f), radius = FingerRadius * 2f, center = center)
    drawCircle(color = color, radius = FingerRadius, center = center)
}
