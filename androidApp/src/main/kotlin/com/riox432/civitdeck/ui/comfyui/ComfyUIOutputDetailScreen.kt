@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.domain.model.ComfyUIGenerationMeta
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.ExpandableTextSection
import com.riox432.civitdeck.ui.components.SectionHeader
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfyUIOutputDetailScreen(
    image: ComfyUIGeneratedImage,
    viewModel: ComfyUIHistoryViewModel,
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
                title = { Text(image.filename, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onSaveImage(image.imageUrl, image.filename) },
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Save to gallery")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        DetailBody(image = image, modifier = Modifier.padding(padding))
    }
}

@Composable
private fun DetailBody(
    image: ComfyUIGeneratedImage,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = Spacing.lg),
        modifier = modifier.fillMaxSize(),
    ) {
        item {
            CivitAsyncImage(
                imageUrl = image.imageUrl,
                contentDescription = image.filename,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(Spacing.md))
        }

        if (image.meta.positivePrompt.isNotBlank()) {
            item { PromptSection(prompt = image.meta.positivePrompt) }
        }

        val hasParams = image.meta.seed != null ||
            image.meta.samplerName != null ||
            image.meta.cfg != null ||
            image.meta.steps != null

        if (hasParams) {
            item { ParamsSection(meta = image.meta) }
        }

        if (image.meta.loraNames.isNotEmpty()) {
            item { LoraSection(loraNames = image.meta.loraNames) }
        }

        item { PromptIdFooter(promptId = image.promptId) }
    }
}

@Composable
private fun PromptSection(prompt: String) {
    SectionHeader(title = "Prompt", modifier = Modifier.padding(horizontal = Spacing.lg))
    ExpandableTextSection(text = prompt, modifier = Modifier.padding(horizontal = Spacing.lg))
    Spacer(modifier = Modifier.height(Spacing.md))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParamsSection(meta: ComfyUIGenerationMeta) {
    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
    Spacer(modifier = Modifier.height(Spacing.sm))
    SectionHeader(
        title = "Generation Settings",
        modifier = Modifier.padding(horizontal = Spacing.lg),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
    ) {
        meta.seed?.let { MetaChip(label = "Seed: $it") }
        meta.samplerName?.let { MetaChip(label = "Sampler: $it") }
        meta.cfg?.let { MetaChip(label = "CFG: $it") }
        meta.steps?.let { MetaChip(label = "Steps: $it") }
    }
    Spacer(modifier = Modifier.height(Spacing.md))
}

@Composable
private fun LoraSection(loraNames: List<String>) {
    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
    Spacer(modifier = Modifier.height(Spacing.sm))
    SectionHeader(title = "LoRAs", modifier = Modifier.padding(horizontal = Spacing.lg))
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
    ) {
        loraNames.forEach { loraName -> LoraBadge(name = loraName) }
    }
    Spacer(modifier = Modifier.height(Spacing.md))
}

@Composable
private fun PromptIdFooter(promptId: String) {
    HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
    Spacer(modifier = Modifier.height(Spacing.sm))
    Text(
        text = "ID: $promptId",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.lg),
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
}

@Composable
private fun MetaChip(label: String) {
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
    )
}

@Composable
private fun LoraBadge(name: String) {
    SuggestionChip(
        onClick = {},
        label = { Text(name, style = MaterialTheme.typography.labelSmall) },
    )
}

