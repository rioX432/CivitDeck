@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryUiState
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

private const val GRID_COLUMNS = 2
private const val IMAGE_ASPECT_RATIO = 1f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfyUIHistoryScreen(
    viewModel: ComfyUIHistoryViewModel,
    onBack: () -> Unit,
    onImageClick: (ComfyUIGeneratedImage) -> Unit,
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
                title = { Text("Output Gallery") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, "Refresh")
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
            modifier = Modifier.padding(padding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryBody(
    state: ComfyUIHistoryUiState,
    images: List<ComfyUIGeneratedImage>,
    onImageClick: (ComfyUIGeneratedImage) -> Unit,
    onRetry: () -> Unit,
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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(GRID_COLUMNS),
                        contentPadding = PaddingValues(Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(images, key = { it.id }) { image ->
                            HistoryImageItem(
                                image = image,
                                onClick = { onImageClick(image) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryImageItem(
    image: ComfyUIGeneratedImage,
    onClick: () -> Unit,
) {
    CivitAsyncImage(
        imageUrl = image.imageUrl,
        contentDescription = image.filename,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(IMAGE_ASPECT_RATIO)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(CornerRadius.image))
            .clickable(onClick = onClick),
    )
}
