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
import com.riox432.civitdeck.domain.model.ComfyUiConnectionDefaults
import com.riox432.civitdeck.domain.model.SDWebUIConnection
import com.riox432.civitdeck.domain.model.SDWebUIConnectionStatus
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun SDWebUISettingsSection(viewModel: SDWebUISettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var nameInput by remember { mutableStateOf("") }
    var hostInput by remember { mutableStateOf(ComfyUiConnectionDefaults.DEFAULT_HOST) }
    var portInput by remember { mutableStateOf(SDWebUIConnection.DEFAULT_SDWEBUI_PORT.toString()) }

    SettingsCard(title = "SD WebUI Server") {
        ConnectionStatusBadge(
            label = "SD WebUI",
            status = state.connectionStatus.name,
            isConnected = state.connectionStatus == SDWebUIConnectionStatus.Connected,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        SDWebUIActiveConnection(state = state, viewModel = viewModel)
        SDWebUISavedConnections(state = state, viewModel = viewModel)

        Spacer(modifier = Modifier.height(Spacing.sm))
        SDWebUIAddConnectionForm(
            nameInput = nameInput,
            hostInput = hostInput,
            portInput = portInput,
            onNameChange = { nameInput = it },
            onHostChange = { hostInput = it },
            onPortChange = { portInput = it },
            onSave = {
                portInput.toIntOrNull()?.let { port ->
                    viewModel.onSaveConnection(nameInput, hostInput, port)
                    nameInput = ""
                    hostInput = ComfyUiConnectionDefaults.DEFAULT_HOST
                    portInput = SDWebUIConnection.DEFAULT_SDWEBUI_PORT.toString()
                }
            },
        )
    }
}

@Composable
private fun SDWebUIAddConnectionForm(
    nameInput: String,
    hostInput: String,
    portInput: String,
    onNameChange: (String) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Text("Add Connection:", style = MaterialTheme.typography.labelMedium)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = nameInput,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = hostInput,
            onValueChange = onHostChange,
            label = { Text("Host") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = portInput,
            onValueChange = onPortChange,
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.width(Spacing.xxl * 3),
        )
    }
    Spacer(modifier = Modifier.height(Spacing.sm))
    Button(
        onClick = onSave,
        enabled = nameInput.isNotBlank() && hostInput.isNotBlank(),
    ) {
        Text("Save Connection")
    }
}

@Composable
private fun SDWebUIActiveConnection(
    state: com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsUiState,
    viewModel: SDWebUISettingsViewModel,
) {
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
}

@Composable
private fun SDWebUISavedConnections(
    state: com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsUiState,
    viewModel: SDWebUISettingsViewModel,
) {
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
}

@Composable
fun SDWebUIGenerationSection(viewModel: SDWebUIGenerationViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "SD WebUI Generation") {
        if (state.isLoading) {
            Text("Loading resources...", style = MaterialTheme.typography.bodySmall)
        } else {
            SDWebUIResourceSelectors(state = state, viewModel = viewModel)
            SDWebUIPromptFields(state = state, viewModel = viewModel)
            Spacer(modifier = Modifier.height(Spacing.sm))
            SDWebUIGenerationParams(state = state, viewModel = viewModel)
            Spacer(modifier = Modifier.height(Spacing.sm))
            SDWebUIDimensionFields(state = state, viewModel = viewModel)
            Spacer(modifier = Modifier.height(Spacing.md))
            SDWebUIGenerateControls(state = state, viewModel = viewModel)
            SDWebUIGenerationFooter(state = state)
        }
    }
}

@Composable
private fun SDWebUIResourceSelectors(
    state: com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationUiState,
    viewModel: SDWebUIGenerationViewModel,
) {
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
}

@Composable
private fun SDWebUIPromptFields(
    state: com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationUiState,
    viewModel: SDWebUIGenerationViewModel,
) {
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
}

@Composable
private fun SDWebUIGenerationParams(
    state: com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationUiState,
    viewModel: SDWebUIGenerationViewModel,
) {
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
}

@Composable
private fun SDWebUIDimensionFields(
    state: com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationUiState,
    viewModel: SDWebUIGenerationViewModel,
) {
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
}

@Composable
private fun SDWebUIGenerateControls(
    state: com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationUiState,
    viewModel: SDWebUIGenerationViewModel,
) {
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
}

@Composable
private fun SDWebUIGenerationFooter(
    state: com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationUiState,
) {
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
