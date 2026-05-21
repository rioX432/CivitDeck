package com.riox432.civitdeck.ui.components.comfyui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Slider row for generation parameters (steps, CFG scale, etc.).
 *
 * Displays a label with the current value on the left and a slider on the right.
 *
 * @param label Display name for the parameter (e.g. "Steps", "CFG Scale")
 * @param valueLabel Formatted string of the current value (e.g. "20", "7.5")
 * @param value Current slider value as Float
 * @param valueRange Allowed range for the slider
 * @param onValueChange Callback when the slider value changes
 * @param modifier Optional modifier for the row
 */
@Composable
fun ParameterSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$label: $valueLabel",
            modifier = Modifier.weight(LABEL_WEIGHT),
            style = MaterialTheme.typography.bodySmall,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(SLIDER_WEIGHT),
        )
    }
}

private const val LABEL_WEIGHT = 0.3f
private const val SLIDER_WEIGHT = 0.7f
