package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.ui.components.comfyui.DimensionInputRow
import com.riox432.civitdeck.ui.components.comfyui.ParameterSliderRow
import com.riox432.civitdeck.ui.components.comfyui.PromptInputFields
import com.riox432.civitdeck.ui.components.comfyui.SeedInputField
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun ComfyUIGenerationSection(viewModel: ComfyUIGenerationViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "ComfyUI Generation") {
        if (state.isLoadingCheckpoints) {
            Text("Loading checkpoints...", style = MaterialTheme.typography.bodySmall)
        } else {
            CheckpointSelector(
                checkpoints = state.checkpoints,
                selectedCheckpoint = state.selectedCheckpoint,
                onCheckpointSelected = viewModel::onCheckpointSelected,
            )
            PromptInputFields(
                prompt = state.prompt,
                negativePrompt = state.negativePrompt,
                onPromptChanged = viewModel::onPromptChanged,
                onNegativePromptChanged = viewModel::onNegativePromptChanged,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            GenerationSliders(
                steps = state.steps,
                cfgScale = state.cfgScale,
                onStepsChanged = { viewModel.onStepsChanged(it.toInt()) },
                onCfgScaleChanged = { viewModel.onCfgScaleChanged(it.toDouble()) },
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            DimensionInputs(
                width = state.width,
                height = state.height,
                seed = state.seed,
                onWidthChanged = viewModel::onWidthChanged,
                onHeightChanged = viewModel::onHeightChanged,
                onSeedChanged = viewModel::onSeedChanged,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            DesktopCustomWorkflowSection(state, viewModel)
            Spacer(modifier = Modifier.height(Spacing.md))
            GenerationProgressBar(
                progressFraction = state.progressFraction,
                currentStep = state.currentStep,
                totalSteps = state.totalSteps,
                isGenerating = state.generationStatus.name == "Running" ||
                    state.generationStatus.name == "Submitting",
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Button(
                onClick = viewModel::onGenerate,
                enabled = state.generationStatus.name != "Running" &&
                    state.generationStatus.name != "Submitting",
            ) {
                Text("Generate")
            }
            GenerationResult(error = state.error, imageCount = state.result?.imageUrls?.size)
        }
    }
}

@Composable
private fun CheckpointSelector(
    checkpoints: List<String>,
    selectedCheckpoint: String,
    onCheckpointSelected: (String) -> Unit,
) {
    if (checkpoints.isNotEmpty()) {
        SettingsDropdown(
            label = "Checkpoint",
            selected = selectedCheckpoint.ifEmpty { "Select..." },
            options = checkpoints,
            onSelected = onCheckpointSelected,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
    }
}

@Composable
private fun GenerationSliders(
    steps: Int,
    cfgScale: Double,
    onStepsChanged: (Float) -> Unit,
    onCfgScaleChanged: (Float) -> Unit,
) {
    ParameterSliderRow(
        label = "Steps",
        valueLabel = steps.toString(),
        value = steps.toFloat(),
        valueRange = STEPS_MIN..STEPS_MAX,
        onValueChange = onStepsChanged,
    )
    ParameterSliderRow(
        label = "CFG Scale",
        valueLabel = "%.1f".format(cfgScale),
        value = cfgScale.toFloat(),
        valueRange = CFG_MIN..CFG_MAX,
        onValueChange = onCfgScaleChanged,
    )
}

@Composable
private fun DimensionInputs(
    width: Int,
    height: Int,
    seed: Long,
    onWidthChanged: (Int) -> Unit,
    onHeightChanged: (Int) -> Unit,
    onSeedChanged: (Long) -> Unit,
) {
    DimensionInputRow(
        width = width,
        height = height,
        onWidthChanged = onWidthChanged,
        onHeightChanged = onHeightChanged,
    )
    SeedInputField(
        seed = seed,
        onSeedChanged = onSeedChanged,
    )
}

@Composable
private fun GenerationResult(
    error: String?,
    imageCount: Int?,
) {
    error?.let {
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    imageCount?.let { count ->
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            "Generation complete! $count image(s)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private const val STEPS_MIN = 1f
private const val STEPS_MAX = 150f
private const val CFG_MIN = 1f
private const val CFG_MAX = 30f
