package com.riox432.civitdeck.ui.externalserver

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerImage
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryUiState
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryViewModel
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalServerGalleryScreen(
    viewModel: ExternalServerGalleryViewModel,
    serverName: String,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 12
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoadingMore) {
            viewModel.onLoadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(serverName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> LoadingStateOverlay()
                state.error != null && state.images.isEmpty() -> ErrorStateView(
                    message = state.error ?: "Failed to load images",
                    onRetry = viewModel::onRetry,
                )
                else -> ImageGrid(
                    state = state,
                    gridState = gridState,
                )
            }
        }
    }
}

@Composable
private fun ImageGrid(
    state: ExternalServerGalleryUiState,
    gridState: LazyGridState,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        contentPadding = PaddingValues(Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(state.images, key = { it.id }) { image ->
            ServerImageCard(image = image)
        }
        if (state.isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ServerImageCard(image: ServerImage) {
    val context = LocalContext.current
    Card {
        Box {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(image.thumbUrl ?: image.file)
                    .crossfade(true)
                    .build(),
                contentDescription = image.character,
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            if (image.aestheticScore != null) {
                Text(
                    text = "%.1f".format(image.aestheticScore),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Spacing.xs),
                )
            }
        }
    }
}
