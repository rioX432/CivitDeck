package com.riox432.civitdeck.ui.dataset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.ui.theme.Spacing
import kotlin.math.roundToInt

private const val MAX_RESOLUTION = 2048f
private const val RESOLUTION_STEPS = 32f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResolutionFilterSheet(
    initialMinWidth: Int = 0,
    initialMinHeight: Int = 0,
    onApply: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var widthSlider by remember { mutableFloatStateOf(initialMinWidth.toFloat()) }
    var heightSlider by remember { mutableFloatStateOf(initialMinHeight.toFloat()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        ResolutionFilterContent(
            widthSlider = widthSlider,
            heightSlider = heightSlider,
            onWidthChange = { widthSlider = it },
            onHeightChange = { heightSlider = it },
            onApply = { onApply(widthSlider.roundToInt(), heightSlider.roundToInt()) },
            onClear = { onApply(0, 0) },
        )
    }
}

@Composable
private fun ResolutionFilterContent(
    widthSlider: Float,
    heightSlider: Float,
    onWidthChange: (Float) -> Unit,
    onHeightChange: (Float) -> Unit,
    onApply: () -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(text = stringResource(R.string.dataset_resolution_filter_title))
        ResolutionSlider(
            label = stringResource(R.string.dataset_resolution_min_width, widthSlider.roundToInt()),
            value = widthSlider,
            onValueChange = onWidthChange,
        )
        ResolutionSlider(
            label = stringResource(R.string.dataset_resolution_min_height, heightSlider.roundToInt()),
            value = heightSlider,
            onValueChange = onHeightChange,
        )
        ResolutionFilterActions(onApply = onApply, onClear = onClear)
    }
}

@Composable
private fun ResolutionSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Text(text = label)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..MAX_RESOLUTION,
            steps = RESOLUTION_STEPS.toInt() - 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ResolutionFilterActions(
    onApply: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.weight(1f),
        ) {
            Text(text = stringResource(R.string.action_clear))
        }
        Button(
            onClick = onApply,
            modifier = Modifier.weight(1f),
        ) {
            Text(text = stringResource(R.string.action_apply))
        }
    }
}
