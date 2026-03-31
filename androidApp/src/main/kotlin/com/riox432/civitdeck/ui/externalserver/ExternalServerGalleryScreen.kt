package com.riox432.civitdeck.ui.externalserver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerImage
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryUiState
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryViewModel
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalServerGalleryScreen(
    viewModel: ExternalServerGalleryViewModel,
    serverName: String,
    onBack: () -> Unit,
    onNavigateToImageDetail: (ServerImage) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    InfiniteScrollEffect(gridState = gridState, state = state, viewModel = viewModel)

    Scaffold(
        topBar = { GalleryTopBar(serverName, state, viewModel, onBack) },
        floatingActionButton = { GenerationFab(state, viewModel) },
    ) { padding ->
        GalleryContent(padding, state, gridState, viewModel, onNavigateToImageDetail)
    }

    GalleryDialogs(state, viewModel)
}

@Composable
private fun InfiniteScrollEffect(
    gridState: LazyGridState,
    state: ExternalServerGalleryUiState,
    viewModel: ExternalServerGalleryViewModel,
) {
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 12
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoadingMore) viewModel.onLoadMore()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryTopBar(
    serverName: String,
    state: ExternalServerGalleryUiState,
    viewModel: ExternalServerGalleryViewModel,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(serverName) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            if (state.supportsFilters) {
                IconButton(onClick = viewModel::onShowFilterSheet) {
                    Icon(Icons.Default.FilterList, "Filters")
                }
            }
        },
    )
}

@Composable
private fun GenerationFab(
    state: ExternalServerGalleryUiState,
    viewModel: ExternalServerGalleryViewModel,
) {
    if (state.supportsGeneration) {
        FloatingActionButton(onClick = viewModel::onShowGenerationSheet) {
            Icon(Icons.Default.Rocket, "Generate")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryContent(
    padding: PaddingValues,
    state: ExternalServerGalleryUiState,
    gridState: LazyGridState,
    viewModel: ExternalServerGalleryViewModel,
    onNavigateToImageDetail: (ServerImage) -> Unit,
) {
    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
        when {
            state.isLoading -> LoadingStateOverlay()
            state.error != null && state.images.isEmpty() -> ErrorStateView(
                message = state.error ?: "Failed to load images",
                onRetry = viewModel::onRetry,
            )
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::onRefresh,
            ) {
                ImageGrid(state = state, gridState = gridState, onImageClick = onNavigateToImageDetail)
            }
        }
    }
}

@Composable
private fun GalleryDialogs(
    state: ExternalServerGalleryUiState,
    viewModel: ExternalServerGalleryViewModel,
) {
    if (state.showFilterSheet) {
        ExternalServerFilterSheet(
            filters = state.filters,
            onFiltersChanged = viewModel::onFiltersChanged,
            onDismiss = viewModel::onDismissFilterSheet,
        )
    }
    if (state.showGenerationSheet) {
        ExternalServerGenerationSheet(
            state = state,
            onParamChanged = viewModel::onGenerationParamChanged,
            onSubmit = viewModel::onSubmitGeneration,
            onDismiss = viewModel::onDismissGenerationSheet,
        )
    }
    state.activeJob?.let { job ->
        GenerationJobDialog(job = job, onDismiss = viewModel::onDismissJobStatus)
    }
}

@Composable
private fun ImageGrid(
    state: ExternalServerGalleryUiState,
    gridState: LazyGridState,
    onImageClick: (ServerImage) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.images, key = { it.id }) { image ->
            ServerImageCard(image = image, onClick = { onImageClick(image) })
        }
        if (state.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun ServerImageCard(image: ServerImage, onClick: () -> Unit) {
    CivitAsyncImage(
        imageUrl = image.thumbUrl ?: image.file,
        contentDescription = image.character,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(CornerRadius.image))
            .clickable(onClick = onClick),
    )
}
