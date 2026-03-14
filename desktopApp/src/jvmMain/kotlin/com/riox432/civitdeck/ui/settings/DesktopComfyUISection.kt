package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.HistorySortOrder
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun ComfyUISettingsSection(viewModel: ComfyUISettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var nameInput by remember { mutableStateOf("") }
    var hostInput by remember { mutableStateOf("127.0.0.1") }
    var portInput by remember { mutableStateOf("8188") }

    SettingsCard(title = "ComfyUI Server") {
        ConnectionStatusBadge(
            label = "ComfyUI",
            status = state.connectionStatus.name,
            isConnected = state.connectionStatus == ComfyUIConnectionStatus.Connected,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        // Active connection
        state.activeConnection?.let { active ->
            Text(
                text = "Active: ${active.name} (${active.hostname}:${active.port})",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(
                    onClick = viewModel::onTestConnection,
                    enabled = !state.isTesting,
                ) {
                    Text(if (state.isTesting) "Testing..." else "Test")
                }
            }
            state.testError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Connection list
        if (state.connections.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text("Saved Connections:", style = MaterialTheme.typography.labelMedium)
            state.connections.forEach { conn ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${conn.name} (${conn.hostname}:${conn.port})",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Row {
                        TextButton(onClick = { viewModel.onActivateConnection(conn.id) }) {
                            Text("Activate")
                        }
                        TextButton(onClick = { viewModel.onDeleteConnection(conn.id) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }

        // Add new connection
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text("Add Connection:", style = MaterialTheme.typography.labelMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = hostInput,
                onValueChange = { hostInput = it },
                label = { Text("Host") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text("Port") },
                singleLine = true,
                modifier = Modifier.width(Spacing.xxl * 3),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Button(
            onClick = {
                val port = portInput.toIntOrNull() ?: return@Button
                viewModel.onSaveConnection(nameInput, hostInput, port)
                nameInput = ""
                hostInput = "127.0.0.1"
                portInput = "8188"
            },
            enabled = nameInput.isNotBlank() && hostInput.isNotBlank(),
        ) {
            Text("Save Connection")
        }
    }
}

@Composable
fun ComfyUIGenerationSection(viewModel: ComfyUIGenerationViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "ComfyUI Generation") {
        if (state.isLoadingCheckpoints) {
            Text("Loading checkpoints...", style = MaterialTheme.typography.bodySmall)
        } else {
            if (state.checkpoints.isNotEmpty()) {
                SettingsDropdown(
                    label = "Checkpoint",
                    selected = state.selectedCheckpoint.ifEmpty { "Select..." },
                    options = state.checkpoints,
                    onSelected = viewModel::onCheckpointSelected,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }
            OutlinedTextField(
                value = state.prompt,
                onValueChange = viewModel::onPromptChanged,
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = state.negativePrompt,
                onValueChange = viewModel::onNegativePromptChanged,
                label = { Text("Negative Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Column(modifier = Modifier.weight(1f)) {
                    SliderSetting(
                        label = "Steps",
                        value = state.steps.toFloat(),
                        valueRange = 1f..150f,
                        steps = 148,
                        valueLabel = state.steps.toString(),
                        onValueChange = { viewModel.onStepsChanged(it.toInt()) },
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    SliderSetting(
                        label = "CFG Scale",
                        value = state.cfgScale.toFloat(),
                        valueRange = 1f..30f,
                        steps = 28,
                        valueLabel = "%.1f".format(state.cfgScale),
                        onValueChange = { viewModel.onCfgScaleChanged(it.toDouble()) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(
                    value = state.width.toString(),
                    onValueChange = { it.toIntOrNull()?.let(viewModel::onWidthChanged) },
                    label = { Text("Width") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.height.toString(),
                    onValueChange = { it.toIntOrNull()?.let(viewModel::onHeightChanged) },
                    label = { Text("Height") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = if (state.seed == -1L) "" else state.seed.toString(),
                    onValueChange = { viewModel.onSeedChanged(it.toLongOrNull() ?: -1L) },
                    label = { Text("Seed (-1 = random)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
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
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            state.result?.let { result ->
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    "Generation complete! ${result.imageUrls.size} image(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun ComfyUIHistorySection(viewModel: ComfyUIHistoryViewModel) {
    val state by viewModel.uiState.collectAsState()
    val images = viewModel.filteredImages()

    SettingsCard(title = "ComfyUI History") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsDropdown(
                label = "Sort",
                selected = state.selectedSort.name,
                options = HistorySortOrder.entries.map { it.name },
                onSelected = { viewModel.onSelectSort(HistorySortOrder.valueOf(it)) },
            )
            TextButton(onClick = viewModel::refresh, enabled = !state.isLoading) {
                Text(if (state.isLoading) "Loading..." else "Refresh")
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        if (images.isEmpty() && !state.isLoading) {
            Text("No history yet", style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                "${images.size} image(s) in history",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        state.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// region Shared Components

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

@Composable
private fun GenerationProgressBar(
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

// endregion
