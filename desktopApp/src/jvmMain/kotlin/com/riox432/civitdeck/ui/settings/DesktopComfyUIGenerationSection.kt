package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
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
            PromptInputs(
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
private fun PromptInputs(
    prompt: String,
    negativePrompt: String,
    onPromptChanged: (String) -> Unit,
    onNegativePromptChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = prompt,
        onValueChange = onPromptChanged,
        label = { Text("Prompt") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 5,
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    OutlinedTextField(
        value = negativePrompt,
        onValueChange = onNegativePromptChanged,
        label = { Text("Negative Prompt") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 3,
    )
}

@Composable
private fun GenerationSliders(
    steps: Int,
    cfgScale: Double,
    onStepsChanged: (Float) -> Unit,
    onCfgScaleChanged: (Float) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Column(modifier = Modifier.weight(1f)) {
            SliderSetting(
                label = "Steps",
                value = steps.toFloat(),
                valueRange = 1f..150f,
                steps = 148,
                valueLabel = steps.toString(),
                onValueChange = onStepsChanged,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            SliderSetting(
                label = "CFG Scale",
                value = cfgScale.toFloat(),
                valueRange = 1f..30f,
                steps = 28,
                valueLabel = "%.1f".format(cfgScale),
                onValueChange = onCfgScaleChanged,
            )
        }
    }
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
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        OutlinedTextField(
            value = width.toString(),
            onValueChange = { it.toIntOrNull()?.let(onWidthChanged) },
            label = { Text("Width") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = height.toString(),
            onValueChange = { it.toIntOrNull()?.let(onHeightChanged) },
            label = { Text("Height") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = if (seed == -1L) "" else seed.toString(),
            onValueChange = { onSeedChanged(it.toLongOrNull() ?: -1L) },
            label = { Text("Seed (-1 = random)") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
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
