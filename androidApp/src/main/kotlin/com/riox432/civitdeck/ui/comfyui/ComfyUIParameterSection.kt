package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationUiState
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.components.comfyui.DimensionInputRow
import com.riox432.civitdeck.ui.components.comfyui.ParameterSliderRow
import com.riox432.civitdeck.ui.components.comfyui.PromptInputFields
import com.riox432.civitdeck.ui.components.comfyui.SeedInputField
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun CheckpointSelector(
    state: GenerationUiState,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(stringResource(R.string.comfyui_checkpoint_label), style = MaterialTheme.typography.labelMedium)
        if (state.isLoadingCheckpoints) {
            LoadingStateOverlay()
        } else {
            TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    state.selectedCheckpoint.ifBlank { "Select checkpoint..." },
                    maxLines = 1,
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.checkpoints.forEach { ckpt ->
                    DropdownMenuItem(
                        text = { Text(ckpt, maxLines = 1) },
                        onClick = {
                            onSelected(ckpt)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun PromptInputs(state: GenerationUiState, viewModel: ComfyUIGenerationViewModel) {
    PromptInputFields(
        prompt = state.prompt,
        negativePrompt = state.negativePrompt,
        onPromptChanged = viewModel::onPromptChanged,
        onNegativePromptChanged = viewModel::onNegativePromptChanged,
        promptLabel = stringResource(R.string.label_prompt),
        negativePromptLabel = stringResource(R.string.label_negative_prompt),
    )
}

@Composable
internal fun ParameterControls(state: GenerationUiState, viewModel: ComfyUIGenerationViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            ParameterSliderRow(
                label = "Steps",
                valueLabel = state.steps.toString(),
                value = state.steps.toFloat(),
                valueRange = STEPS_MIN..STEPS_MAX,
                onValueChange = { viewModel.onStepsChanged(it.toInt()) },
            )
            ParameterSliderRow(
                label = "CFG Scale",
                valueLabel = "%.1f".format(state.cfgScale),
                value = state.cfgScale.toFloat(),
                valueRange = CFG_MIN..CFG_MAX,
                onValueChange = { viewModel.onCfgScaleChanged(it.toDouble()) },
            )
            DimensionInputRow(
                width = state.width,
                height = state.height,
                onWidthChanged = viewModel::onWidthChanged,
                onHeightChanged = viewModel::onHeightChanged,
                widthLabel = stringResource(R.string.comfyui_width_label),
                heightLabel = stringResource(R.string.comfyui_height_label),
            )
            SeedInputField(
                seed = state.seed,
                onSeedChanged = viewModel::onSeedChanged,
                label = stringResource(R.string.comfyui_seed_label),
            )
        }
    }
}

private const val STEPS_MIN = 1f
private const val STEPS_MAX = 150f
private const val CFG_MIN = 1f
private const val CFG_MAX = 30f
