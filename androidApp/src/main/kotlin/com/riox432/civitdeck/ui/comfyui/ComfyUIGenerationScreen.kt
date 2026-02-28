@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.LoraSelection
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationUiState
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfyUIGenerationScreen(
    viewModel: ComfyUIGenerationViewModel,
    onBack: () -> Unit,
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
            if (state.result?.imageUrls?.isNotEmpty() == true) {
                item { ResultGrid(state.result!!.imageUrls, viewModel::onSaveImage) }
            }
        }
    }
}

@Composable
private fun CheckpointSelector(
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
private fun PromptInputs(state: GenerationUiState, viewModel: ComfyUIGenerationViewModel) {
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
private fun ParameterControls(state: GenerationUiState, viewModel: ComfyUIGenerationViewModel) {
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
private fun SliderRow(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
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
private fun SliderRow(label: String, value: Double, min: Double, max: Double, onChange: (Double) -> Unit) {
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

@Composable
private fun GenerateButton(state: GenerationUiState, onGenerate: () -> Unit) {
    val isGenerating = state.generationStatus == GenerationStatus.Submitting ||
        state.generationStatus == GenerationStatus.Running
    Button(
        onClick = onGenerate,
        enabled = !isGenerating && state.selectedCheckpoint.isNotBlank() && state.prompt.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isGenerating) {
            CircularProgressIndicator(modifier = Modifier.padding(end = Spacing.sm))
        }
        Text(if (isGenerating) "Generating..." else "Generate")
    }
}

@Composable
private fun GenerationStatusSection(state: GenerationUiState) {
    when (state.generationStatus) {
        GenerationStatus.Running -> GenerationProgressSection(state)
        GenerationStatus.Error -> {
            Text(
                state.error ?: "Generation failed",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        GenerationStatus.Completed -> {
            Text(
                "Generation complete!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        else -> {}
    }
}

@Composable
private fun GenerationProgressSection(state: GenerationUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        if (state.totalSteps > 0) {
            LinearProgressIndicator(
                progress = { state.progressFraction },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Step ${state.currentStep} / ${state.totalSteps}",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Generating...", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ResultGrid(imageUrls: List<String>, onSaveImage: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        userScrollEnabled = false,
    ) {
        items(imageUrls) { url ->
            Card {
                Column {
                    CivitAsyncImage(
                        imageUrl = url,
                        contentDescription = "Generated image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                    )
                    IconButton(
                        onClick = { onSaveImage(url) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Save to gallery")
                    }
                }
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
                            onClick = { viewModel.onLoraAdded(lora); expanded = false },
                        )
                    }
                }
            }
            state.loraSelections.forEach { lora ->
                LoraRow(lora, viewModel)
            }
            if (state.availableLoras.isEmpty() && !state.isLoadingLoras) {
                Text("No LoRAs found on server", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun LoraRow(lora: LoraSelection, viewModel: ComfyUIGenerationViewModel) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(lora.name.substringAfterLast('/'), modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall, maxLines = 1)
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
                            onClick = { viewModel.onControlNetSelected(cn); expanded = false },
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
                Text("Custom Workflow JSON", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                if (state.customWorkflowJson != null) {
                    IconButton(onClick = viewModel::onClearCustomWorkflow) {
                        Icon(Icons.Default.Close, "Clear workflow")
                    }
                }
            }
            if (state.customWorkflowJson != null) {
                Text("Custom workflow loaded (${state.customWorkflowJson.length} chars)",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
            onConfirm = { viewModel.onImportWorkflow(inputText); showDialog = false },
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
