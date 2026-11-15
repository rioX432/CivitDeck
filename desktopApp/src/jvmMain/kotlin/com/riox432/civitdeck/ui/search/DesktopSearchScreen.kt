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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.feature.search.presentation.ModelSearchViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.ui.desktopFocusRing
import com.riox432.civitdeck.ui.theme.Spacing
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Suppress("CyclomaticComplexMethod", "UnusedParameter")
@Composable
fun DesktopSearchScreen(
    viewModel: ModelSearchViewModel,
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
    val gridState = rememberLazyGridState()
    var showBaseModelPicker by remember { mutableStateOf(false) }

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
        SearchTopBar(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::onSearch,
            onQRCodeClick = onQRCodeClick,
            onUrlImportClick = onUrlImportClick,
            searchFocusRequester = searchFocusRequester,
        )

        DesktopFilterBar(
            uiState = uiState,
            onTypeSelected = viewModel::onTypeSelected,
            onSortSelected = viewModel::onSortSelected,
            onPeriodSelected = viewModel::onPeriodSelected,
            onBaseModelToggled = viewModel::onBaseModelToggled,
            onChooseBaseModels = { showBaseModelPicker = true },
            onQualityFilterToggled = viewModel::onQualityFilterToggled,
            onSourceToggled = viewModel::toggleSource,
            onNsfwLevelSelected = viewModel::onNsfwFilterLevelSelected,
            onResetFilters = viewModel::resetFilters,
        )

        SearchResultsArea(
            uiState = uiState,
            displayState = displayState,
            gridState = gridState,
            onModelClick = onModelClick,
            onRetry = viewModel::onSearch,
            onRefresh = viewModel::refresh,
        )
    }

    if (showBaseModelPicker) {
        DesktopBaseModelPicker(
            catalog = uiState.baseModelCatalog,
            initialSelection = uiState.selectedBaseModels,
            onApply = { selection ->
                viewModel.onBaseModelsApplied(selection)
                showBaseModelPicker = false
            },
            onDismiss = { showBaseModelPicker = false },
        )
    }
}

@Composable
private fun SearchResultsArea(
    uiState: com.riox432.civitdeck.feature.search.presentation.ModelSearchUiState,
    displayState: com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsUiState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onModelClick: (Long) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val openModel: (Model) -> Unit = { model ->
        val detailId = model.reference.nativeDetailId
        if (detailId != null) {
            onModelClick(detailId)
        } else {
            // External IDs are synthetic, so they never enter native detail. openUri throws when
            // the OS has no handler for the link.
            val url = model.reference.canonicalUrl
            val opened = url != null && runCatching { uriHandler.openUri(url) }.isSuccess
            if (!opened) scope.launch { snackbarHostState.showSnackbar(EXTERNAL_LINK_UNAVAILABLE) }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (uiState.sourceNotices.isNotEmpty()) {
                DesktopSearchSourceNotices(
                    notices = uiState.sourceNotices,
                    onRefresh = onRefresh,
                    modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.sm),
                )
            }
            Box(Modifier.weight(1f)) {
                SearchResultsState(
                    uiState = uiState,
                    displayState = displayState,
                    gridState = gridState,
                    onModelClick = openModel,
                    onRetry = onRetry,
                )
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SearchResultsState(
    uiState: com.riox432.civitdeck.feature.search.presentation.ModelSearchUiState,
    displayState: com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsUiState,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onModelClick: (Model) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        uiState.isLoading && uiState.models.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null && uiState.models.isEmpty() -> {
            SearchErrorState(error = uiState.error, onRetry = onRetry)
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
            // NSFW filtering is fully handled by the shared ViewModel (server
            // param + client-side level narrowing) — no local re-filtering.
            SearchResultsGrid(
                models = uiState.models,
                columns = displayState.gridColumns,
                isLoadingMore = uiState.isLoadingMore,
                gridState = gridState,
                onModelClick = onModelClick,
            )
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onQRCodeClick: () -> Unit,
    onUrlImportClick: () -> Unit,
    searchFocusRequester: FocusRequester,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        DesktopSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = onSearch,
            focusRequester = searchFocusRequester,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onQRCodeClick, modifier = Modifier.desktopFocusRing()) {
            Icon(
                Icons.Default.QrCode,
                contentDescription = "QR Code",
            )
        }
        IconButton(onClick = onUrlImportClick, modifier = Modifier.desktopFocusRing()) {
            Icon(
                Icons.Default.Link,
                contentDescription = "Import from URL",
            )
        }
    }
}

@Composable
private fun SearchErrorState(
    error: String?,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = error ?: "Unknown error",
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun SearchResultsGrid(
    models: List<Model>,
    columns: Int,
    isLoadingMore: Boolean,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onModelClick: (Model) -> Unit,
) {
    LazyVerticalGrid(
        columns = if (columns > 0) {
            GridCells.Fixed(
                columns
            )
        } else {
            GridCells.Adaptive(minSize = CARD_MIN_WIDTH)
        },
        state = gridState,
        contentPadding = PaddingValues(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(
            items = models,
            key = { index, model -> model.gridKey(index) },
        ) { _, model ->
            DesktopModelCard(
                model = model,
                onClick = { onModelClick(model) },
            )
        }
        if (isLoadingMore) {
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

/**
 * Lazy-grid key unique across providers: external providers' numeric IDs are synthetic and can
 * equal a CivitAI ID. A legacy external model has no exact identity, so its position stands in.
 */
private fun Model.gridKey(index: Int): String =
    reference.stableKey ?: "legacy:${source.name}:$index"

private val CARD_MIN_WIDTH = 256.dp
private const val LOAD_MORE_THRESHOLD = 5
private const val EXTERNAL_LINK_UNAVAILABLE = "This model has no web page to open."
