@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationUiState
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SDWebUIGenerationScreen(
    viewModel: SDWebUIGenerationViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onDismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SD WebUI Generation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        SDWebUIGenerationContent(
            state = state,
            modifier = Modifier.padding(padding),
            onPromptChanged = viewModel::onPromptChanged,
            onNegativePromptChanged = viewModel::onNegativePromptChanged,
            onModelSelected = viewModel::onModelSelected,
            onSamplerSelected = viewModel::onSamplerSelected,
            onStepsChanged = viewModel::onStepsChanged,
            onCfgChanged = viewModel::onCfgChanged,
            onWidthChanged = viewModel::onWidthChanged,
            onHeightChanged = viewModel::onHeightChanged,
            onSeedChanged = viewModel::onSeedChanged,
            onGenerate = viewModel::onGenerate,
            onInterrupt = viewModel::onInterrupt,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun SDWebUIGenerationContent(
    state: SDWebUIGenerationUiState,
    modifier: Modifier = Modifier,
    onPromptChanged: (String) -> Unit,
    onNegativePromptChanged: (String) -> Unit,
    onModelSelected: (String) -> Unit,
    onSamplerSelected: (String) -> Unit,
    onStepsChanged: (Int) -> Unit,
    onCfgChanged: (Double) -> Unit,
    onWidthChanged: (Int) -> Unit,
    onHeightChanged: (Int) -> Unit,
    onSeedChanged: (Long) -> Unit,
    onGenerate: () -> Unit,
    onInterrupt: () -> Unit,
) {
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        sdwebuiFormItems(
            state, onModelSelected, onSamplerSelected,
            onPromptChanged, onNegativePromptChanged,
            onStepsChanged, onCfgChanged, onWidthChanged, onHeightChanged, onSeedChanged,
        )
        item {
            SDWebUIGenerateButton(
                isGenerating = state.isGenerating,
                promptBlank = state.prompt.isBlank(),
                progress = state.progress,
                step = state.progressStep,
                totalSteps = state.progressTotalSteps,
                onGenerate = onGenerate,
                onInterrupt = onInterrupt,
            )
        }
        if (state.generatedImages.isNotEmpty()) {
            item { SDWebUIResultGrid(state.generatedImages) }
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.sdwebuiFormItems(
    state: SDWebUIGenerationUiState,
    onModelSelected: (String) -> Unit,
    onSamplerSelected: (String) -> Unit,
    onPromptChanged: (String) -> Unit,
    onNegativePromptChanged: (String) -> Unit,
    onStepsChanged: (Int) -> Unit,
    onCfgChanged: (Double) -> Unit,
    onWidthChanged: (Int) -> Unit,
    onHeightChanged: (Int) -> Unit,
    onSeedChanged: (Long) -> Unit,
) {
    item { SDWebUIDropdownRow("Model", state.selectedModel, state.models, onModelSelected) }
    item { SDWebUIDropdownRow("Sampler", state.selectedSampler, state.samplers, onSamplerSelected) }
    item {
        OutlinedTextField(
            value = state.prompt,
            onValueChange = onPromptChanged,
            label = { Text("Prompt") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item {
        OutlinedTextField(
            value = state.negativePrompt,
            onValueChange = onNegativePromptChanged,
            label = { Text("Negative Prompt") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    item { SDWebUIStepsSection(state.steps, onStepsChanged) }
    item { SDWebUICfgSection(state.cfgScale, onCfgChanged) }
    item {
        SDWebUIDimensionRow(
            width = state.width,
            height = state.height,
            onWidthChanged = onWidthChanged,
            onHeightChanged = onHeightChanged,
        )
    }
    item { SDWebUISeedRow(state.seed, onSeedChanged) }
}

@Composable
private fun SDWebUIDropdownRow(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(selected.ifBlank { "Select $label" }, modifier = Modifier.weight(1f))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SDWebUIStepsSection(steps: Int, onChanged: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Steps", style = MaterialTheme.typography.labelMedium)
            Text("$steps", style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = steps.toFloat(),
            onValueChange = { onChanged(it.toInt()) },
            valueRange = 1f..50f,
            steps = 48,
        )
    }
}

@Composable
private fun SDWebUICfgSection(cfg: Double, onChanged: (Double) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("CFG Scale", style = MaterialTheme.typography.labelMedium)
            Text(String.format(java.util.Locale.US, "%.1f", cfg), style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = cfg.toFloat(),
            onValueChange = { onChanged(it.toDouble()) },
            valueRange = 1f..20f,
            steps = 18,
        )
    }
}

@Composable
private fun SDWebUIDimensionRow(
    width: Int,
    height: Int,
    onWidthChanged: (Int) -> Unit,
    onHeightChanged: (Int) -> Unit,
) {
    val sizes = listOf(256, 512, 768, 1024)
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            SDWebUIDropdownRow("Width", "$width", sizes.map { "$it" }) { v ->
                onWidthChanged(v.toInt())
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            SDWebUIDropdownRow("Height", "$height", sizes.map { "$it" }) { v ->
                onHeightChanged(v.toInt())
            }
        }
    }
}

@Composable
private fun SDWebUISeedRow(seed: Long, onChanged: (Long) -> Unit) {
    var text by remember(seed) { mutableStateOf(seed.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { v ->
            text = v
            v.toLongOrNull()?.let { onChanged(it) }
        },
        label = { Text("Seed (-1 = random)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SDWebUIGenerateButton(
    isGenerating: Boolean,
    promptBlank: Boolean,
    progress: Double,
    step: Int,
    totalSteps: Int,
    onGenerate: () -> Unit,
    onInterrupt: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (isGenerating) {
            Text(
                if (totalSteps > 0) "Step $step / $totalSteps" else "Generating...",
                style = MaterialTheme.typography.bodySmall,
            )
            LinearProgressIndicator(
                progress = { progress.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = onInterrupt, modifier = Modifier.fillMaxWidth()) {
                Text("Interrupt")
            }
        } else {
            Button(
                onClick = onGenerate,
                enabled = !promptBlank,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Generate")
            }
        }
    }
}

@Composable
private fun SDWebUIResultGrid(base64Images: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Generated Images", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            base64Images.forEach { b64 ->
                Box(modifier = Modifier.weight(1f)) {
                    SDWebUIBase64Image(b64)
                }
            }
        }
    }
}

@Composable
private fun SDWebUIBase64Image(base64: String) {
    val bitmap = remember(base64) {
        runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = stringResource(R.string.cd_generated_image),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
    } else {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text("Image error", style = MaterialTheme.typography.bodySmall)
        }
    }
}
