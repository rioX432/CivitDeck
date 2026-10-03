package com.riox432.civitdeck.ui.comfyui

import androidx.annotation.StringRes
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.riox432.civitdeck.R
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationModelSource
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationUiState
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.components.comfyui.DimensionInputRow
import com.riox432.civitdeck.ui.components.comfyui.ParameterSliderRow
import com.riox432.civitdeck.ui.components.comfyui.PromptInputFields
import com.riox432.civitdeck.ui.components.comfyui.SeedInputField
import com.riox432.civitdeck.ui.theme.Spacing

/**
 * Checkpoint picker that becomes a grouped "Model" picker once the server lists diffusion models,
 * so a server without them shows the same form as before.
 */
@Composable
internal fun ModelSelector(
    state: GenerationUiState,
    onCheckpointSelected: (String) -> Unit,
    onDiffusionModelSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val grouped = state.diffusionModels.isNotEmpty()
    val selectedModel = when (state.modelSource) {
        GenerationModelSource.CHECKPOINT -> state.selectedCheckpoint
        GenerationModelSource.DIFFUSION_MODEL -> state.selectedDiffusionModel
    }
    Column {
        Text(
            stringResource(if (grouped) R.string.comfyui_model_label else R.string.comfyui_checkpoint_label),
            style = MaterialTheme.typography.labelMedium,
        )
        if (state.isLoadingCheckpoints) {
            LoadingStateOverlay()
        } else {
            val placeholder = stringResource(
                if (grouped) R.string.comfyui_select_model else R.string.comfyui_select_checkpoint,
            )
            TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedModel.ifBlank { placeholder }, maxLines = 1)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                // Each group calls its own handler, so a file name present in both folders stays distinct.
                ModelMenuGroup(
                    header = if (grouped) R.string.comfyui_checkpoints_header else null,
                    models = state.checkpoints,
                    onSelected = {
                        onCheckpointSelected(it)
                        expanded = false
                    },
                )
                ModelMenuGroup(
                    header = R.string.comfyui_diffusion_models_header,
                    models = state.diffusionModels,
                    onSelected = {
                        onDiffusionModelSelected(it)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ModelMenuGroup(
    @StringRes header: Int?,
    models: List<String>,
    onSelected: (String) -> Unit,
) {
    if (models.isEmpty()) return
    if (header != null) {
        Text(
            stringResource(header),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                .semantics { heading() },
        )
    }
    models.forEach { model ->
        DropdownMenuItem(
            text = { Text(model, maxLines = 1) },
            onClick = { onSelected(model) },
        )
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
