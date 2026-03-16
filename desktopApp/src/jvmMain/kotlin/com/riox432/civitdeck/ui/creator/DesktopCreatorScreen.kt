package com.riox432.civitdeck.ui.creator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.riox432.civitdeck.feature.creator.presentation.CreatorProfileViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.search.DesktopModelCard
import com.riox432.civitdeck.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DesktopCreatorScreen(
    viewModel: CreatorProfileViewModel,
    onBack: () -> Unit,
    onModelClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Top bar
        Surface(tonalElevation = Elevation.xs) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = uiState.username,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = viewModel::toggleFollow) {
                    Text(if (uiState.isFollowing) "Unfollow" else "Follow")
                }
            }
        }

        when {
            uiState.isLoading -> LoadingStateOverlay()
            uiState.error != null && uiState.models.isEmpty() -> ErrorStateView(
                message = uiState.error ?: "Unknown error",
                onRetry = viewModel::refresh,
            )
            !uiState.isLoading && uiState.models.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No models found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> CreatorModelGrid(
                viewModel = viewModel,
                onModelClick = onModelClick,
            )
        }
    }
}

@Composable
private fun CreatorModelGrid(
    viewModel: CreatorProfileViewModel,
    onModelClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val displayViewModel: DisplaySettingsViewModel = koinViewModel()
    val displayState by displayViewModel.uiState.collectAsState()
    val gridColumns = displayState.gridColumns
    val gridState = rememberLazyGridState()

    // Trigger load more near the end
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo }
            .collect { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = layoutInfo.totalItemsCount
                if (lastVisible >= total - LOAD_MORE_THRESHOLD && uiState.hasMore) {
                    viewModel.loadMore()
                }
            }
    }

    LazyVerticalGrid(
        columns = if (gridColumns > 0) GridCells.Fixed(gridColumns) else GridCells.Adaptive(minSize = CARD_MIN_WIDTH),
        state = gridState,
        contentPadding = PaddingValues(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        itemsIndexed(uiState.models, key = { _, model -> model.id }) { _, model ->
            DesktopModelCard(
                model = model,
                onClick = { onModelClick(model.id) },
            )
        }
        if (uiState.isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        if (!uiState.hasMore && uiState.models.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "All models loaded",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private val CARD_MIN_WIDTH = 240.dp
private const val LOAD_MORE_THRESHOLD = 5
