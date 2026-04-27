package com.riox432.civitdeck.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.input.KeyboardType
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
import com.riox432.civitdeck.feature.comfyui.domain.model.ExtractedParameter
import com.riox432.civitdeck.feature.comfyui.domain.model.ParameterType
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationUiState
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.HistorySortOrder
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun ComfyUISettingsSection(viewModel: ComfyUISettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var nameInput by remember { mutableStateOf("") }
    var hostInput by remember { mutableStateOf("127.0.0.1") }
    var portInput by remember { mutableStateOf(ComfyUIConnection.DEFAULT_COMFYUI_PORT.toString()) }
    var useHttpsInput by remember { mutableStateOf(false) }

    SettingsCard(title = "ComfyUI Server") {
        ConnectionStatusBadge(
            label = "ComfyUI",
            status = state.connectionStatus.name,
            isConnected = state.connectionStatus == ComfyUIConnectionStatus.Connected,
        )
        // Security level indicator
        state.securityLevel?.let { level ->
            Text(
                text = when (level) {
                    com.riox432.civitdeck.domain.model.ConnectionSecurityLevel.Secure -> "Secure (HTTPS)"
                    com.riox432.civitdeck.domain.model.ConnectionSecurityLevel.SelfSigned -> "Self-signed certificate"
                    com.riox432.civitdeck.domain.model.ConnectionSecurityLevel.LocalInsecure -> "LAN (HTTP)"
                    com.riox432.civitdeck.domain.model.ConnectionSecurityLevel.RemoteInsecure -> "Warning: HTTP over internet"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when (level) {
                    com.riox432.civitdeck.domain.model.ConnectionSecurityLevel.Secure ->
                        MaterialTheme.colorScheme.primary
                    com.riox432.civitdeck.domain.model.ConnectionSecurityLevel.RemoteInsecure ->
                        MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))

        // Scan LAN
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedButton(
                onClick = viewModel::onScanLan,
                enabled = !state.isScanning,
            ) {
                Text(if (state.isScanning) "Scanning..." else "Scan LAN")
            }
        }
        if (state.discoveredServers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            state.discoveredServers.forEach { server ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${server.displayName} (${server.ip}:${server.port})",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.onSelectDiscoveredServer(server) }) {
                        Text("Add")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))

        // Active connection
        state.activeConnection?.let { active ->
            Text(
                text = "Active: ${active.name} (${active.baseUrl})",
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
                        text = "${conn.name} (${conn.baseUrl})",
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            androidx.compose.material3.Checkbox(
                checked = useHttpsInput,
                onCheckedChange = { useHttpsInput = it },
            )
            Text("Use HTTPS", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Button(
            onClick = {
                val port = portInput.toIntOrNull() ?: return@Button
                viewModel.onSaveConnection(nameInput, hostInput, port, useHttpsInput, false)
                nameInput = ""
                hostInput = "127.0.0.1"
                portInput = ComfyUIConnection.DEFAULT_COMFYUI_PORT.toString()
                useHttpsInput = false
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
            // Custom Workflow
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

// region Custom Workflow + Parameter Editing

@Composable
private fun DesktopCustomWorkflowSection(
    state: GenerationUiState,
    viewModel: ComfyUIGenerationViewModel,
) {
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showParams by remember { mutableStateOf(false) }

    Text("Custom Workflow", style = MaterialTheme.typography.labelMedium)
    Spacer(modifier = Modifier.height(Spacing.xs))
    val customJson = state.customWorkflowJson
    if (customJson != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Workflow loaded (${customJson.length} chars)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            TextButton(onClick = viewModel::onClearCustomWorkflow) {
                Text("Clear")
            }
        }
        if (state.extractedParameters.isNotEmpty()) {
            OutlinedButton(onClick = { showParams = !showParams }) {
                Text(
                    if (showParams) "Hide Parameters" else "Edit Parameters (${state.extractedParameters.size})",
                )
            }
        } else if (state.isLoadingParameters) {
            Text(
                "Loading parameters...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = showParams && state.extractedParameters.isNotEmpty()) {
            DesktopParameterEditor(state.extractedParameters, viewModel)
        }
    } else {
        OutlinedButton(onClick = { showImport = !showImport }) {
            Text(if (showImport) "Cancel Import" else "Import Workflow JSON")
        }
    }
    state.workflowImportError?.let { err ->
        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    if (showImport && customJson == null) {
        Spacer(modifier = Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = importText,
            onValueChange = { importText = it },
            label = { Text("Workflow JSON") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 8,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Button(onClick = {
            viewModel.onImportWorkflow(importText)
            showImport = false
        }) {
            Text("Import")
        }
    }
}

@Composable
private fun DesktopParameterEditor(
    parameters: List<ExtractedParameter>,
    viewModel: ComfyUIGenerationViewModel,
) {
    val grouped = remember(parameters) {
        parameters.groupBy { it.nodeId to it.nodeTitle }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Spacer(modifier = Modifier.height(Spacing.xs))
        grouped.forEach { (nodeKey, nodeParams) ->
            DesktopNodeParamGroup(nodeKey.second, nodeParams, viewModel)
        }
    }
}

@Composable
private fun DesktopNodeParamGroup(
    nodeTitle: String,
    parameters: List<ExtractedParameter>,
    viewModel: ComfyUIGenerationViewModel,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(nodeTitle, style = MaterialTheme.typography.labelMedium)
            parameters.forEach { param ->
                DesktopParamWidget(param, viewModel::onParameterValueChanged)
            }
        }
    }
}

@Composable
private fun DesktopParamWidget(
    param: ExtractedParameter,
    onChanged: (String, String, String) -> Unit,
) {
    when (param.paramType) {
        ParameterType.TEXT -> {
            OutlinedTextField(
                value = param.currentValue,
                onValueChange = { onChanged(param.nodeId, param.paramName, it) },
                label = { Text(param.paramName) },
                modifier = Modifier.fillMaxWidth(),
                minLines = if (param.paramName == "text") 2 else 1,
                maxLines = if (param.paramName == "text") 6 else 2,
            )
        }
        ParameterType.NUMBER -> {
            val min = param.min
            val max = param.max
            if (min != null && max != null && max > min) {
                DesktopNumberSlider(param, min, max, onChanged)
            } else {
                OutlinedTextField(
                    value = param.currentValue,
                    onValueChange = { onChanged(param.nodeId, param.paramName, it) },
                    label = { Text(param.paramName) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
        ParameterType.SELECT -> {
            DesktopParamDropdown(param, onChanged)
        }
        ParameterType.SEED -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = param.currentValue,
                    onValueChange = { onChanged(param.nodeId, param.paramName, it) },
                    label = { Text(param.paramName) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedButton(onClick = {
                    val seed = kotlin.random.Random.nextLong(0, Long.MAX_VALUE)
                    onChanged(param.nodeId, param.paramName, seed.toString())
                }) {
                    Text("Random")
                }
            }
        }
    }
}

@Composable
private fun DesktopNumberSlider(
    param: ExtractedParameter,
    min: Double,
    max: Double,
    onChanged: (String, String, String) -> Unit,
) {
    val currentFloat = param.currentValue.toFloatOrNull() ?: min.toFloat()
    val isInteger = param.step != null && param.step >= 1.0
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(param.paramName, style = MaterialTheme.typography.bodySmall)
            Text(
                if (isInteger) currentFloat.toInt().toString() else "%.2f".format(currentFloat),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Slider(
            value = currentFloat.coerceIn(min.toFloat(), max.toFloat()),
            onValueChange = { newVal ->
                val formatted = if (isInteger) newVal.toInt().toString() else "%.2f".format(newVal)
                onChanged(param.nodeId, param.paramName, formatted)
            },
            valueRange = min.toFloat()..max.toFloat(),
        )
    }
}

@Composable
private fun DesktopParamDropdown(
    param: ExtractedParameter,
    onChanged: (String, String, String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(param.paramName, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { expanded = true }) {
            Text(param.currentValue.ifBlank { "Select..." }, maxLines = 1)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            param.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.substringAfterLast('/'), maxLines = 1) },
                    onClick = {
                        onChanged(param.nodeId, param.paramName, option)
                        expanded = false
                    },
                )
            }
        }
    }
}

// endregion

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
