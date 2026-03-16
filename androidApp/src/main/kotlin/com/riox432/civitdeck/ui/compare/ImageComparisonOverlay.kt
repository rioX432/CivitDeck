package com.riox432.civitdeck.ui.compare

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.riox432.civitdeck.R
import com.riox432.civitdeck.ui.theme.CivitDeckColors

/**
 * Full-screen overlay for comparing two images with a slider.
 *
 * Shows a close button, orientation toggle, and before/after labels.
 */
@Composable
fun ImageComparisonOverlay(
    beforeImageUrl: String,
    afterImageUrl: String,
    beforeLabel: String = "Before",
    afterLabel: String = "After",
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BackHandler(onBack = onDismiss)
        OverlayContent(beforeImageUrl, afterImageUrl, beforeLabel, afterLabel, onDismiss)
    }
}

@Composable
private fun OverlayContent(
    beforeImageUrl: String,
    afterImageUrl: String,
    beforeLabel: String,
    afterLabel: String,
    onDismiss: () -> Unit,
) {
    var orientation by remember { mutableStateOf(SliderOrientation.Horizontal) }
    var controlsVisible by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize().background(CivitDeckColors.scrim)) {
        ImageComparisonSlider(
            beforeImageUrl = beforeImageUrl,
            afterImageUrl = afterImageUrl,
            orientation = orientation,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
            OverlayControls(
                orientation = orientation,
                beforeLabel = beforeLabel,
                afterLabel = afterLabel,
                onToggleOrientation = {
                    orientation = when (orientation) {
                        SliderOrientation.Horizontal -> SliderOrientation.Vertical
                        SliderOrientation.Vertical -> SliderOrientation.Horizontal
                    }
                },
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun OverlayControls(
    orientation: SliderOrientation,
    beforeLabel: String,
    afterLabel: String,
    onToggleOrientation: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top bar: close + orientation toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(CONTROL_PADDING.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OverlayIconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close))
            }
            OverlayIconButton(onClick = onToggleOrientation) {
                val iconRes = when (orientation) {
                    SliderOrientation.Horizontal -> R.drawable.ic_slider_vertical
                    SliderOrientation.Vertical -> R.drawable.ic_slider_horizontal
                }
                Icon(painterResource(id = iconRes), contentDescription = stringResource(R.string.cd_toggle_orientation))
            }
        }

        // Bottom labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(CONTROL_PADDING.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LabelChip(text = beforeLabel)
            LabelChip(text = afterLabel)
        }
    }
}

@Composable
private fun OverlayIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = CHIP_ALPHA),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        content()
    }
}

@Composable
private fun LabelChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = CivitDeckColors.onScrim,
        modifier = Modifier
            .background(CivitDeckColors.scrim.copy(alpha = CHIP_ALPHA), MaterialTheme.shapes.small)
            .padding(horizontal = CHIP_H_PADDING.dp, vertical = CHIP_V_PADDING.dp),
    )
}

private const val CONTROL_PADDING = 16
private const val CHIP_ALPHA = 0.7f
private const val CHIP_H_PADDING = 12
private const val CHIP_V_PADDING = 6
