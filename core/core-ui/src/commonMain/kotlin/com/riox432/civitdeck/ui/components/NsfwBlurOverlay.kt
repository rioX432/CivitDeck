package com.riox432.civitdeck.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwLevel

/**
 * Wraps [content] with a blur overlay based on the NSFW level and blur settings.
 * Supports tap-to-reveal: tapping the blurred area temporarily removes the blur.
 * Resets the revealed state when the blur radius changes so that slider adjustments
 * in Settings immediately take effect.
 */
@Composable
fun NsfwBlurOverlay(
    nsfwLevel: NsfwLevel,
    blurSettings: NsfwBlurSettings,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val blurRadius = blurSettings.blurRadiusFor(nsfwLevel)
    var isRevealed by remember { mutableStateOf(false) }
    // Reset reveal state when the blur radius changes so that slider adjustments
    // in Settings take effect immediately instead of being hidden by a prior reveal.
    LaunchedEffect(blurRadius) { isRevealed = false }
    val effectiveBlur = if (isRevealed) 0f else blurRadius

    Box(modifier = modifier) {
        Box(modifier = Modifier.blur(effectiveBlur.dp)) {
            content()
        }

        // Tap-to-reveal overlay when blurred
        AnimatedVisibility(
            visible = effectiveBlur > 0f,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(role = Role.Button, onClickLabel = "Reveal content") { isRevealed = true },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Tap to reveal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
    }
}
