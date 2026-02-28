package com.riox432.civitdeck.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

private const val SHIMMER_DURATION_MS = 1200

@Composable
fun Modifier.shimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslateX",
    )

    val colorBase = MaterialTheme.colorScheme.surfaceVariant
    val colorHighlight = MaterialTheme.colorScheme.surface

    val brush = Brush.linearGradient(
        colors = listOf(colorBase, colorHighlight, colorBase),
        start = Offset(translateX * 1000f, 0f),
        end = Offset(translateX * 1000f + 1000f, 0f),
    )

    return this.background(brush)
}
