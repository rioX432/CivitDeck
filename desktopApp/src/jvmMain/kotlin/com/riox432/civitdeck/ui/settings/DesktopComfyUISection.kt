package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Shared badge showing connection status with colored indicator.
 * Used by ComfyUI, External Server, and SD WebUI sections.
 */
@Composable
internal fun ConnectionStatusBadge(
    label: String,
    status: String,
    isConnected: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label: ", style = MaterialTheme.typography.labelMedium)
        Text(
            text = status,
            style = MaterialTheme.typography.bodySmall,
            color = if (isConnected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/**
 * Progress bar for image generation with step counter.
 */
@Composable
internal fun GenerationProgressBar(
    progressFraction: Float,
    currentStep: Int,
    totalSteps: Int,
    isGenerating: Boolean,
) {
    if (isGenerating) {
        Column {
            if (totalSteps > 0) {
                Text(
                    "Step $currentStep / $totalSteps",
                    style = MaterialTheme.typography.bodySmall,
                )
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text("Generating...", style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
