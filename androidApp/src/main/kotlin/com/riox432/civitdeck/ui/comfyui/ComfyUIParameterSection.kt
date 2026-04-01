package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationUiState
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun CheckpointSelector(
    state: GenerationUiState,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("Checkpoint", style = MaterialTheme.typography.labelMedium)
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
    OutlinedTextField(
        value = state.prompt,
        onValueChange = viewModel::onPromptChanged,
        label = { Text("Prompt") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        maxLines = 6,
    )
    OutlinedTextField(
        value = state.negativePrompt,
        onValueChange = viewModel::onNegativePromptChanged,
        label = { Text("Negative Prompt") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 4,
    )
}

@Composable
internal fun ParameterControls(state: GenerationUiState, viewModel: ComfyUIGenerationViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            SliderRow("Steps", state.steps, 1, 150) { viewModel.onStepsChanged(it) }
            SliderRow(
                "CFG Scale",
                state.cfgScale,
                1.0,
                30.0,
            ) { viewModel.onCfgScaleChanged(it) }
            ResolutionRow(state.width, state.height, viewModel)
            SeedInput(state.seed, viewModel::onSeedChanged)
        }
    }
}

@Composable
internal fun SliderRow(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$label: $value", modifier = Modifier.weight(0.3f), style = MaterialTheme.typography.bodySmall)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            modifier = Modifier.weight(0.7f),
        )
    }
}

@Composable
internal fun SliderRow(label: String, value: Double, min: Double, max: Double, onChange: (Double) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$label: ${"%.1f".format(value)}",
            modifier = Modifier.weight(0.3f),
            style = MaterialTheme.typography.bodySmall,
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toDouble()) },
            valueRange = min.toFloat()..max.toFloat(),
            modifier = Modifier.weight(0.7f),
        )
    }
}

@Composable
private fun ResolutionRow(
    width: Int,
    height: Int,
    viewModel: ComfyUIGenerationViewModel,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = width.toString(),
            onValueChange = { it.toIntOrNull()?.let(viewModel::onWidthChanged) },
            label = { Text("Width") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        OutlinedTextField(
            value = height.toString(),
            onValueChange = { it.toIntOrNull()?.let(viewModel::onHeightChanged) },
            label = { Text("Height") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
    }
}

@Composable
private fun SeedInput(seed: Long, onChanged: (Long) -> Unit) {
    OutlinedTextField(
        value = if (seed == -1L) "" else seed.toString(),
        onValueChange = {
            val parsed = it.toLongOrNull() ?: -1L
            onChanged(parsed)
        },
        label = { Text("Seed (-1 = random)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}
