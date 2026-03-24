package com.riox432.civitdeck.ui.search

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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DesktopSearchScreen(
    viewModel: DesktopSearchViewModel,
    onModelClick: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
    onUrlImportClick: () -> Unit = {},
    onQRCodeClick: () -> Unit = {},
    searchFocusRequester: FocusRequester = FocusRequester(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val displayViewModel: DisplaySettingsViewModel = koinViewModel()
    val displayState by displayViewModel.uiState.collectAsState()
    val contentFilterVm: ContentFilterSettingsViewModel = koinViewModel()
    val contentFilterState by contentFilterVm.uiState.collectAsState()
    val gridState = rememberLazyGridState()

    // Trigger load more near the end
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo }
            .collect { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = layoutInfo.totalItemsCount
                if (total > 0 && lastVisible >= total - LOAD_MORE_THRESHOLD) {
                    viewModel.loadMore()
                }
            }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            DesktopSearchBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = viewModel::onSearch,
                focusRequester = searchFocusRequester,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onQRCodeClick) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = "QR Code",
                )
            }
            IconButton(onClick = onUrlImportClick) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = "Import from URL",
                )
            }
        }

        DesktopFilterBar(
            uiState = uiState,
            onTypeSelected = viewModel::onTypeSelected,
            onSortSelected = viewModel::onSortSelected,
            onPeriodSelected = viewModel::onPeriodSelected,
            onBaseModelToggled = viewModel::onBaseModelToggled,
            onQualityFilterToggled = viewModel::onQualityFilterToggled,
            onSourceToggled = viewModel::toggleSource,
            onResetFilters = viewModel::resetFilters,
        )

        when {
            uiState.isLoading && uiState.models.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null && uiState.models.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = viewModel::onSearch) {
                            Text("Retry")
                        }
                    }
                }
            }
            !uiState.isLoading && uiState.error == null && uiState.models.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No models found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                val columns = displayState.gridColumns
                val nsfwFilterLevel = contentFilterState.nsfwFilterLevel
                val filteredModels = if (nsfwFilterLevel == NsfwFilterLevel.All) {
                    uiState.models.filter { !it.nsfw }
                } else {
                    uiState.models
                }
                val nsfwBlurSettings = contentFilterState.nsfwBlurSettings

                LazyVerticalGrid(
                    columns = if (columns > 0) GridCells.Fixed(columns) else GridCells.Adaptive(minSize = CARD_MIN_WIDTH),
                    state = gridState,
                    contentPadding = PaddingValues(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(
                        items = filteredModels,
                        key = { _, model -> model.id },
                    ) { _, model ->
                        DesktopModelCard(
                            model = model,
                            onClick = { onModelClick(model.id) },
                            nsfwBlurSettings = nsfwBlurSettings,
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
                }
            }
        }
    }
}

private val CARD_MIN_WIDTH = 256.dp
private const val LOAD_MORE_THRESHOLD = 5
