package com.riox432.civitdeck.ui.comfyui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.domain.model.LoraSelection
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationModelSource
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationUiState
import com.riox432.civitdeck.ui.components.comfyui.ParameterSliderRow
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfyUIGenerationScreen(
    viewModel: ComfyUIGenerationViewModel,
    onBack: () -> Unit,
    generationNotificationsEnabled: Boolean,
    onLoadTemplate: (() -> Unit)? = null,
    onNavigateToMaskEditor: ((String, Int, Int) -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    SaveResultSnackbar(state.imageSaveSuccess, snackbarHostState, viewModel::onDismissSaveResult)
    val onGenerate = rememberGenerateWithNotificationPrompt(generationNotificationsEnabled, viewModel::onGenerate)

    val isGenerating = state.generationStatus == GenerationStatus.Submitting ||
        state.generationStatus == GenerationStatus.Running

    Scaffold(
        topBar = {
            GenerationTopBar(onBack, onLoadTemplate, isGenerating, state)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        GenerationContent(padding, state, viewModel, onGenerate, onNavigateToMaskEditor)
    }
}

// Generation alerts are on by default, so most users never flip the settings switch that requests
// POST_NOTIFICATIONS and the completion alert is silently dropped on Android 13+. Starting a job is
// the in-context moment Android recommends for the prompt; the result never gates the generation.
// Asking at most once per screen avoids nagging, since the system makes a second denial final.
@Composable
private fun rememberGenerateWithNotificationPrompt(
    generationNotificationsEnabled: Boolean,
    onGenerate: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    var permissionPrompted by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return {
        onGenerate()
        val shouldPrompt = shouldPromptForGenerationNotifications(
            generationNotificationsEnabled = generationNotificationsEnabled,
            alreadyPrompted = permissionPrompted,
            sdkInt = Build.VERSION.SDK_INT,
            permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
        if (shouldPrompt) {
            permissionPrompted = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

internal fun shouldPromptForGenerationNotifications(
    generationNotificationsEnabled: Boolean,
    alreadyPrompted: Boolean,
    sdkInt: Int,
    permissionGranted: Boolean,
): Boolean = generationNotificationsEnabled &&
    !alreadyPrompted &&
    sdkInt >= Build.VERSION_CODES.TIRAMISU &&
    !permissionGranted

@Composable
private fun SaveResultSnackbar(
    imageSaveSuccess: Boolean?,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    val savedMessage = stringResource(R.string.comfyui_image_saved)
    val failedMessage = stringResource(R.string.comfyui_image_save_failed)
    LaunchedEffect(imageSaveSuccess) {
        when (imageSaveSuccess) {
            true -> {
                snackbarHostState.showSnackbar(savedMessage)
                onDismiss()
            }
            false -> {
                snackbarHostState.showSnackbar(failedMessage)
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
            title = { Text(stringResource(R.string.comfyui_txt2img_title)) },
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
    onGenerate: () -> Unit,
    onNavigateToMaskEditor: ((String, Int, Int) -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.padding(padding),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Keyed because the diffusion-model items come and go; with positional keys, every section
        // that shifts would lose its remembered state, such as the workflow JSON draft.
        item(key = "model") {
            ModelSelector(
                state = state,
                onCheckpointSelected = viewModel::onCheckpointSelected,
                onDiffusionModelSelected = viewModel::selectDiffusionModelForBuiltInWorkflow,
            )
        }
        val isDiffusionModel = state.modelSource == GenerationModelSource.DIFFUSION_MODEL
        if (isDiffusionModel) {
            item(key = "diffusionModel") { DiffusionModelSelector(state, viewModel) }
        }
        item(key = "prompt") { PromptInputs(state, viewModel) }
        item(key = "parameters") { ParameterControls(state, viewModel) }
        item(key = "lora") { LoraSection(state, viewModel) }
        if (!isDiffusionModel) {
            item(key = "controlNet") { ControlNetSection(state, viewModel) }
            item(key = "inpainting") {
                InpaintingSection(state, viewModel, onNavigateToMaskEditor)
            }
        }
        item(key = "customWorkflow") { CustomWorkflowSection(state, viewModel) }
        item(key = "generate") { GenerateButton(state, onGenerate, viewModel::onInterrupt) }
        item(key = "status") { GenerationStatusSection(state) }
        val result = state.result
        if (result?.imageUrls?.isNotEmpty() == true) {
            item(key = "result") { ResultGrid(result.imageUrls, viewModel::onSaveImage) }
        }
    }
}

// The built-in workflow has no ControlNet or inpainting graph for a diffusion model and rejects the
// request, and both sections are hidden while one is selected, so a setting left over from a
// checkpoint would make Generate fail with no visible way to undo it.
private fun ComfyUIGenerationViewModel.selectDiffusionModelForBuiltInWorkflow(model: String) {
    onControlNetToggled(false)
    onClearMask()
    onDiffusionModelSelected(model)
}

@Composable
private fun LoraSection(state: GenerationUiState, viewModel: ComfyUIGenerationViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.comfyui_lora_label),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
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
                    stringResource(R.string.comfyui_no_loras),
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
        ParameterSliderRow(
            label = "Strength",
            valueLabel = "%.2f".format(lora.strengthModel),
            value = lora.strengthModel,
            valueRange = 0f..2f,
            onValueChange = { v -> viewModel.onLoraStrengthChanged(lora.name, v, v) },
        )
    }
}

@Composable
private fun ControlNetSection(state: GenerationUiState, viewModel: ComfyUIGenerationViewModel) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.comfyui_controlnet_label),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
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
                ParameterSliderRow(
                    label = "Strength",
                    valueLabel = "%.2f".format(state.controlNetStrength),
                    value = state.controlNetStrength,
                    valueRange = 0f..2f,
                    onValueChange = { viewModel.onControlNetStrengthChanged(it) },
                )
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
                Text(stringResource(R.string.comfyui_edit_parameters_count, state.extractedParameters.size))
            }
        } else if (state.isLoadingParameters) {
            Text(
                stringResource(R.string.comfyui_loading_parameters),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Button(
            onClick = onImport,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.comfyui_import_workflow_json)) }
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
        title = { Text(stringResource(R.string.comfyui_paste_workflow_json)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.comfyui_workflow_json_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                maxLines = 12,
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text(stringResource(R.string.action_import)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
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
                Text(stringResource(R.string.action_clear))
            }
        }
        ParameterSliderRow(
            label = "Denoise",
            valueLabel = "%.2f".format(state.denoiseStrength),
            value = state.denoiseStrength.toFloat(),
            valueRange = 0f..1f,
            onValueChange = { viewModel.onDenoiseStrengthChanged(it.toDouble()) },
        )
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
            Text(stringResource(R.string.comfyui_add_mask))
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
