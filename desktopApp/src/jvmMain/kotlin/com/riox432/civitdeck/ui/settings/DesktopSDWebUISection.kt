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
import com.riox432.civitdeck.domain.model.SDWebUIConnectionStatus
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun SDWebUISettingsSection(viewModel: SDWebUISettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var nameInput by remember { mutableStateOf("") }
    var hostInput by remember { mutableStateOf("127.0.0.1") }
    var portInput by remember { mutableStateOf("7860") }

    SettingsCard(title = "SD WebUI Server") {
        ConnectionStatusBadge(
            label = "SD WebUI",
            status = state.connectionStatus.name,
            isConnected = state.connectionStatus == SDWebUIConnectionStatus.Connected,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        state.activeConnection?.let { active ->
            Text(
                text = "Active: ${active.name} (${active.hostname}:${active.port})",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick = viewModel::onTestConnection,
                enabled = !state.isTesting,
            ) {
                Text(if (state.isTesting) "Testing..." else "Test")
            }
            state.testError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

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
                        "${conn.name} (${conn.hostname}:${conn.port})",
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
                portInput = "7860"
            },
            enabled = nameInput.isNotBlank() && hostInput.isNotBlank(),
        ) {
            Text("Save Connection")
        }
    }
}

@Composable
fun SDWebUIGenerationSection(viewModel: SDWebUIGenerationViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "SD WebUI Generation") {
        if (state.isLoading) {
            Text("Loading resources...", style = MaterialTheme.typography.bodySmall)
        } else {
            if (state.models.isNotEmpty()) {
                SettingsDropdown(
                    label = "Model",
                    selected = state.selectedModel.ifEmpty { "Select..." },
                    options = state.models,
                    onSelected = viewModel::onModelSelected,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
            }
            if (state.samplers.isNotEmpty()) {
                SettingsDropdown(
                    label = "Sampler",
                    selected = state.selectedSampler,
                    options = state.samplers,
                    onSelected = viewModel::onSamplerSelected,
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
                        onValueChange = { viewModel.onCfgChanged(it.toDouble()) },
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
            if (state.isGenerating) {
                LinearProgressIndicator(
                    progress = { state.progress.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        "Generating... ${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = viewModel::onInterrupt) {
                        Text("Interrupt")
                    }
                }
            } else {
                Button(
                    onClick = viewModel::onGenerate,
                    enabled = state.prompt.isNotBlank(),
                ) {
                    Text("Generate")
                }
            }
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (state.generatedImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    "${state.generatedImages.size} image(s) generated",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
