@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryUiState
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.HistorySortOrder
import com.riox432.civitdeck.ui.adaptive.adaptiveGridColumns
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.FilterChipRow
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.dataset.AddToDatasetSheet
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

private const val IMAGE_ASPECT_RATIO = 1f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfyUIHistoryScreen(
    viewModel: ComfyUIHistoryViewModel,
    onBack: () -> Unit,
    onImageClick: (ComfyUIGeneratedImage) -> Unit,
    scrollToTopTrigger: Int = 0,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val datasets by viewModel.datasets.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val gridState = rememberLazyGridState()
    var lastHandledTrigger by rememberSaveable { mutableIntStateOf(scrollToTopTrigger) }
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger != lastHandledTrigger) {
            lastHandledTrigger = scrollToTopTrigger
            gridState.animateScrollToItem(0)
        }
    }

    HistorySnackbarEffects(state = state, snackbarHostState = snackbarHostState, viewModel = viewModel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Output Gallery") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_refresh))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HistoryBody(
            state = state,
            images = viewModel.filteredImages(),
            onImageClick = onImageClick,
            onRetry = viewModel::refresh,
            onSortSelected = viewModel::onSelectSort,
            onAddToDataset = viewModel::onAddToDatasetTap,
            gridState = gridState,
            modifier = Modifier.padding(padding),
        )
    }

    if (state.showDatasetPicker) {
        AddToDatasetSheet(
            datasets = datasets,
            onSelectDataset = viewModel::onDatasetSelected,
            onCreateAndSelect = viewModel::onCreateDatasetAndSelect,
            onDismiss = viewModel::onDismissDatasetPicker,
        )
    }
}

@Composable
private fun HistorySnackbarEffects(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryBody(
    state: ComfyUIHistoryUiState,
    images: List<ComfyUIGeneratedImage>,
    onImageClick: (ComfyUIGeneratedImage) -> Unit,
    onRetry: () -> Unit,
    onSortSelected: (HistorySortOrder) -> Unit,
    onAddToDataset: (ComfyUIGeneratedImage) -> Unit,
    gridState: LazyGridState = rememberLazyGridState(),
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading && images.isEmpty() -> LoadingStateOverlay()
            state.error != null && images.isEmpty() -> {
                ErrorStateView(
                    message = state.error ?: "Error",
                    onRetry = onRetry,
                )
            }
            images.isEmpty() -> {
                EmptyStateMessage(
                    icon = Icons.Default.BrokenImage,
                    title = "No outputs yet",
                    subtitle = "Generated images will appear here after running workflows.",
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = onRetry,
                ) {
                    val gridColumns = adaptiveGridColumns()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            FilterHeader(
                                selectedSort = state.selectedSort,
                                onSortSelected = onSortSelected,
                            )
                        }
                        items(images, key = { it.id }) { image ->
                            HistoryImageItem(
                                image = image,
                                onClick = { onImageClick(image) },
                                onAddToDataset = { onAddToDataset(image) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterHeader(
    selectedSort: HistorySortOrder,
    onSortSelected: (HistorySortOrder) -> Unit,
) {
    FilterChipRow(
        options = HistorySortOrder.entries,
        selected = selectedSort,
        onSelect = onSortSelected,
        label = { it.name },
        modifier = Modifier.padding(bottom = Spacing.xs),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryImageItem(
    image: ComfyUIGeneratedImage,
    onClick: () -> Unit,
    onAddToDataset: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        CivitAsyncImage(
            imageUrl = image.imageUrl,
            contentDescription = image.filename,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(IMAGE_ASPECT_RATIO)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(CornerRadius.image))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true },
                ),
        )
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Add to Dataset") },
                onClick = {
                    showMenu = false
                    onAddToDataset()
                },
            )
        }
    }
}
