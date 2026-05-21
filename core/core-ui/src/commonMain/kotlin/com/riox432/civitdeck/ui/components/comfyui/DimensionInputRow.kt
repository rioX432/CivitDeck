package com.riox432.civitdeck.ui.components.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.ui.theme.Spacing

/**
 * Width and height text fields for image dimension input.
 *
 * @param width Current width value
 * @param height Current height value
 * @param onWidthChanged Callback when width changes
 * @param onHeightChanged Callback when height changes
 * @param widthLabel Label for the width field
 * @param heightLabel Label for the height field
 * @param modifier Optional modifier for the row
 */
@Composable
fun DimensionInputRow(
    width: Int,
    height: Int,
    onWidthChanged: (Int) -> Unit,
    onHeightChanged: (Int) -> Unit,
    widthLabel: String = "Width",
    heightLabel: String = "Height",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = width.toString(),
            onValueChange = { it.toIntOrNull()?.let(onWidthChanged) },
            label = { Text(widthLabel) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        OutlinedTextField(
            value = height.toString(),
            onValueChange = { it.toIntOrNull()?.let(onHeightChanged) },
            label = { Text(heightLabel) },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
    }
}
