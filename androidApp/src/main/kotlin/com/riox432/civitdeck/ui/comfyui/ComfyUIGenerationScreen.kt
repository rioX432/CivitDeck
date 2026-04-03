@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.LoraSelection
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfyUIGenerationScreen(
    viewModel: ComfyUIGenerationViewModel,
    onBack: () -> Unit,
    onLoadTemplate: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.imageSaveSuccess) {
        when (state.imageSaveSuccess) {
            true -> {
                snackbarHostState.showSnackbar("Image saved to gallery")
                viewModel.onDismissSaveResult()
            }
            false -> {
                snackbarHostState.showSnackbar("Failed to save image")
                viewModel.onDismissSaveResult()
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("txt2img") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    onLoadTemplate?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.FolderOpen, "Load template")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { CheckpointSelector(state, viewModel::onCheckpointSelected) }
            item { PromptInputs(state, viewModel) }
            item { ParameterControls(state, viewModel) }
            item { LoraSection(state, viewModel) }
            item { ControlNetSection(state, viewModel) }
            item { CustomWorkflowSection(state, viewModel) }
            item { GenerateButton(state, viewModel::onGenerate) }
            item { GenerationStatusSection(state) }
            val result = state.result
            if (result?.imageUrls?.isNotEmpty() == true) {
                item { ResultGrid(result.imageUrls, viewModel::onSaveImage) }
            }
        }
    }
}

@Composable
private fun LoraSection(state: GenerationUiState, viewModel: ComfyUIGenerationViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LoRA", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = { expanded = true }, enabled = state.availableLoras.isNotEmpty()) {
                    Icon(Icons.Default.Add, "Add LoRA")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    state.availableLoras.forEach { lora ->
                        DropdownMenuItem(
                            text = { Text(lora.substringAfterLast('/'), maxLines = 1) },
                            onClick = {
                                viewModel.onLoraAdded(lora)
                                expanded = false
                            },
                        )
                    }
                }
            }
            state.loraSelections.forEach { lora ->
                LoraRow(lora, viewModel)
            }
            if (state.availableLoras.isEmpty() && !state.isLoadingLoras) {
                Text(
                    "No LoRAs found on server",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoraRow(lora: LoraSelection, viewModel: ComfyUIGenerationViewModel) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                lora.name.substringAfterLast('/'),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
            IconButton(onClick = { viewModel.onLoraRemoved(lora.name) }) {
                Icon(Icons.Default.Close, "Remove LoRA")
            }
        }
        SliderRow(
            label = "Strength: ${"%.2f".format(lora.strengthModel)}",
            value = lora.strengthModel.toDouble(),
            min = 0.0,
            max = 2.0,
        ) { v -> viewModel.onLoraStrengthChanged(lora.name, v.toFloat(), v.toFloat()) }
    }
}

@Composable
private fun ControlNetSection(state: GenerationUiState, viewModel: ComfyUIGenerationViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ControlNet", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Switch(checked = state.controlNetEnabled, onCheckedChange = viewModel::onControlNetToggled)
            }
            if (state.controlNetEnabled) {
                TextButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(state.selectedControlNet.ifBlank { "Select ControlNet model..." }, maxLines = 1)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    state.availableControlNets.forEach { cn ->
                        DropdownMenuItem(
                            text = { Text(cn.substringAfterLast('/'), maxLines = 1) },
                            onClick = {
                                viewModel.onControlNetSelected(cn)
                                expanded = false
                            },
                        )
                    }
                }
                SliderRow("Strength", state.controlNetStrength.toDouble(), 0.0, 2.0) { v ->
                    viewModel.onControlNetStrengthChanged(v.toFloat())
                }
            }
        }
    }
}

@Composable
private fun CustomWorkflowSection(state: GenerationUiState, viewModel: ComfyUIGenerationViewModel) {
    var showDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Custom Workflow JSON",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                if (state.customWorkflowJson != null) {
                    IconButton(onClick = viewModel::onClearCustomWorkflow) {
                        Icon(Icons.Default.Close, "Clear workflow")
                    }
                }
            }
            val customJson = state.customWorkflowJson
            if (customJson != null) {
                Text(
                    "Custom workflow loaded (${customJson.length} chars)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Import Workflow JSON")
                }
            }
            state.workflowImportError?.let { err ->
                Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDialog) {
        WorkflowImportDialog(
            text = inputText,
            onTextChange = { inputText = it },
            onConfirm = {
                viewModel.onImportWorkflow(inputText)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun WorkflowImportDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste ComfyUI Workflow JSON") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("Workflow JSON") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                maxLines = 12,
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Import") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
