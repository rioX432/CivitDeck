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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.LoraSelection
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationUiState
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfyUIGenerationScreen(
    viewModel: ComfyUIGenerationViewModel,
    onBack: () -> Unit,
    onLoadTemplate: (() -> Unit)? = null,
    onNavigateToMaskEditor: ((String, Int, Int) -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    SaveResultSnackbar(state.imageSaveSuccess, snackbarHostState, viewModel::onDismissSaveResult)

    val isGenerating = state.generationStatus == GenerationStatus.Submitting ||
        state.generationStatus == GenerationStatus.Running

    Scaffold(
        topBar = {
            GenerationTopBar(onBack, onLoadTemplate, isGenerating, state)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        GenerationContent(padding, state, viewModel, onNavigateToMaskEditor)
    }
}

@Composable
private fun SaveResultSnackbar(
    imageSaveSuccess: Boolean?,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(imageSaveSuccess) {
        when (imageSaveSuccess) {
            true -> {
                snackbarHostState.showSnackbar("Image saved to gallery")
                onDismiss()
            }
            false -> {
                snackbarHostState.showSnackbar("Failed to save image")
                onDismiss()
            }
            null -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerationTopBar(
    onBack: () -> Unit,
    onLoadTemplate: (() -> Unit)?,
    isGenerating: Boolean,
    state: GenerationUiState,
) {
    Column {
        TopAppBar(
            title = { Text("txt2img") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_navigate_back),
                    )
                }
            },
            actions = {
                onLoadTemplate?.let {
                    IconButton(onClick = it) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = stringResource(R.string.cd_load_template),
                        )
                    }
                }
            },
        )
        if (isGenerating) {
            TopBarProgress(state)
        }
    }
}

@Composable
private fun GenerationContent(
    padding: PaddingValues,
    state: GenerationUiState,
    viewModel: ComfyUIGenerationViewModel,
    onNavigateToMaskEditor: ((String, Int, Int) -> Unit)?,
) {
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
        item {
            InpaintingSection(state, viewModel, onNavigateToMaskEditor)
        }
        item { CustomWorkflowSection(state, viewModel) }
        item { GenerateButton(state, viewModel::onGenerate, viewModel::onInterrupt) }
        item { GenerationStatusSection(state) }
        val result = state.result
        if (result?.imageUrls?.isNotEmpty() == true) {
            item { ResultGrid(result.imageUrls, viewModel::onSaveImage) }
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
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_lora))
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
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_remove_lora))
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
    var showParameterSheet by remember { mutableStateOf(false) }

    CustomWorkflowCard(state, viewModel, onImport = { showDialog = true }, onEditParams = { showParameterSheet = true })

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
    if (showParameterSheet) {
        WorkflowParameterSheet(
            parameters = state.extractedParameters,
            onParameterChanged = viewModel::onParameterValueChanged,
            onRefresh = viewModel::onRefreshParameters,
            onDismiss = { showParameterSheet = false },
        )
    }
}

@Composable
private fun CustomWorkflowCard(
    state: GenerationUiState,
    viewModel: ComfyUIGenerationViewModel,
    onImport: () -> Unit,
    onEditParams: () -> Unit,
) {
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
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_clear_workflow))
                    }
                }
            }
            CustomWorkflowContent(state, onImport, onEditParams)
            state.workflowImportError?.let { err ->
                Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CustomWorkflowContent(state: GenerationUiState, onImport: () -> Unit, onEditParams: () -> Unit) {
    val customJson = state.customWorkflowJson
    if (customJson != null) {
        Text(
            "Custom workflow loaded (${customJson.length} chars)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        if (state.extractedParameters.isNotEmpty()) {
            Button(onClick = onEditParams, modifier = Modifier.fillMaxWidth()) {
                Text("Edit Parameters (${state.extractedParameters.size})")
            }
        } else if (state.isLoadingParameters) {
            Text(
                "Loading parameters...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Import Workflow JSON") }
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

@Composable
private fun InpaintingSection(
    state: GenerationUiState,
    viewModel: ComfyUIGenerationViewModel,
    onNavigateToMaskEditor: ((String, Int, Int) -> Unit)?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                "Inpainting Mask",
                style = MaterialTheme.typography.labelLarge,
            )
            InpaintingMaskContent(state, viewModel, onNavigateToMaskEditor)
        }
    }
}

@Composable
private fun InpaintingMaskContent(
    state: GenerationUiState,
    viewModel: ComfyUIGenerationViewModel,
    onNavigateToMaskEditor: ((String, Int, Int) -> Unit)?,
) {
    val hasMask = state.maskImageFilename != null
    if (hasMask) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Mask: ${state.maskImageFilename}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = viewModel::onClearMask) {
                Text("Clear")
            }
        }
        SliderRow(
            "Denoise",
            state.denoiseStrength,
            0.0,
            1.0,
        ) { viewModel.onDenoiseStrengthChanged(it) }
    } else {
        Button(
            onClick = {
                // Use current generation dimensions as mask size
                onNavigateToMaskEditor?.invoke(
                    "",
                    state.width,
                    state.height,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Add Mask")
        }
    }
}

@Composable
private fun TopBarProgress(state: GenerationUiState) {
    if (state.totalSteps > 0) {
        LinearProgressIndicator(
            progress = { state.progressFraction },
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}
