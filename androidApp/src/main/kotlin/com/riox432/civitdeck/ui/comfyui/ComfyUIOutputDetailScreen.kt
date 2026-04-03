@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.domain.model.ComfyUIGenerationMeta
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryUiState
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.ExpandableTextSection
import com.riox432.civitdeck.ui.components.SectionHeader
import com.riox432.civitdeck.ui.dataset.AddToDatasetSheet
import com.riox432.civitdeck.ui.gallery.ImageViewerOverlay
import com.riox432.civitdeck.ui.gallery.ViewerImage
import com.riox432.civitdeck.ui.share.SocialShareSheet
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfyUIOutputDetailScreen(
    images: List<ComfyUIGeneratedImage>,
    initialIndex: Int,
    viewModel: ComfyUIHistoryViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val datasets by viewModel.datasets.collectAsStateWithLifecycle()
    val hashtags by viewModel.shareHashtags.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
    val currentImage = images[pagerState.currentPage]
    DetailSnackbarEffects(state = state, snackbarHostState = snackbarHostState, viewModel = viewModel)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${pagerState.currentPage + 1} / ${images.size}", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_share))
                    }
                    IconButton(onClick = { viewModel.onAddToDatasetTap(currentImage) }) {
                        Icon(Icons.Default.Dataset, contentDescription = stringResource(R.string.cd_add_to_dataset))
                    }
                    IconButton(
                        onClick = { viewModel.onSaveImage(currentImage.imageUrl, currentImage.filename) },
                    ) {
                        Icon(Icons.Default.Download, contentDescription = stringResource(R.string.cd_save_to_gallery))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HorizontalPager(state = pagerState, modifier = Modifier.padding(padding)) { page ->
            DetailPage(image = images[page])
        }
    }
    if (state.showDatasetPicker) {
        AddToDatasetSheet(
            datasets = datasets,
            onSelectDataset = viewModel::onDatasetSelected,
            onCreateAndSelect = viewModel::onCreateDatasetAndSelect,
            onDismiss = viewModel::onDismissDatasetPicker,
        )
    }
    if (showShareSheet) {
        SocialShareSheet(
            hashtags = hashtags,
            onToggleHashtag = viewModel::onToggleShareHashtag,
            onAddHashtag = viewModel::onAddShareHashtag,
            onRemoveHashtag = viewModel::onRemoveShareHashtag,
            onDismiss = { showShareSheet = false },
        )
    }
}

@Composable
private fun DetailPage(image: ComfyUIGeneratedImage) {
    var showImageViewer by rememberSaveable { mutableStateOf(false) }
    DetailBody(image = image, onImageClick = { showImageViewer = true })
    if (showImageViewer) {
        ImageViewerOverlay(
            images = listOf(ViewerImage(url = image.imageUrl)),
            initialIndex = 0,
            onDismiss = { showImageViewer = false },
        )
    }
}

@Composable
private fun DetailSnackbarEffects(
    state: ComfyUIHistoryUiState,
    snackbarHostState: SnackbarHostState,
    viewModel: ComfyUIHistoryViewModel,
) {
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
    LaunchedEffect(state.addToDatasetSuccess) {
        when (state.addToDatasetSuccess) {
            true -> {
                snackbarHostState.showSnackbar("Added to dataset")
                viewModel.onDismissDatasetResult()
            }
            false -> {
                snackbarHostState.showSnackbar("Failed to add to dataset")
                viewModel.onDismissDatasetResult()
            }
            null -> {}
        }
    }
}

@Composable
private fun DetailBody(
    image: ComfyUIGeneratedImage,
    modifier: Modifier = Modifier,
    onImageClick: () -> Unit = {},
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onImageClick, onClickLabel = "View image details"),
            )
            Spacer(modifier = Modifier.height(Spacing.md))
        }

        val showPrompt = image.meta.positivePrompt.isNotBlank()
        val hasParams = image.meta.seed != null ||
            image.meta.samplerName != null ||
            image.meta.cfg != null ||
            image.meta.steps != null
        val showLoras = image.meta.loraNames.isNotEmpty()

        if (showPrompt) {
            item { PromptSection(prompt = image.meta.positivePrompt) }
        }

        if (hasParams) {
            item { ParamsSection(meta = image.meta, showTopDivider = showPrompt) }
        }

        if (showLoras) {
            item { LoraSection(loraNames = image.meta.loraNames, showTopDivider = showPrompt || hasParams) }
        }

        item { PromptIdFooter(promptId = image.promptId, showTopDivider = showPrompt || hasParams || showLoras) }
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
private fun ParamsSection(meta: ComfyUIGenerationMeta, showTopDivider: Boolean = true) {
    if (showTopDivider) HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
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
private fun LoraSection(loraNames: List<String>, showTopDivider: Boolean = true) {
    if (showTopDivider) HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
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
private fun PromptIdFooter(promptId: String, showTopDivider: Boolean = true) {
    if (showTopDivider) HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
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
