package com.riox432.civitdeck.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.riox432.civitdeck.R
import com.riox432.civitdeck.feature.search.presentation.ModelSearchViewModel
import com.riox432.civitdeck.ui.adaptive.adaptiveGridColumns
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun ModelSearchScreen(
    viewModel: ModelSearchViewModel,
    onModelClick: (Long, String?, String) -> Unit = { _, _, _ -> },
    scrollToTopTrigger: Int = 0,
    compareModelName: String? = null,
    onCancelCompare: () -> Unit = {},
    onDiscoverClick: () -> Unit = {},
    onCompareModel: (Long, String) -> Unit = { _, _ -> },
    onScanQRCode: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val userGridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val gridColumns = adaptiveGridColumns(userGridColumns)
    val ownedHashes by viewModel.ownedHashes.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.pagingData.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    val headerState = rememberCollapsibleHeaderState()

    HeaderSnapEffect(gridState = gridState, headerState = headerState)

    var lastHandledTrigger by rememberSaveable { mutableIntStateOf(scrollToTopTrigger) }
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger != lastHandledTrigger) {
            lastHandledTrigger = scrollToTopTrigger
            gridState.animateScrollToItem(0)
            headerState.offsetPx = 0f
        }
    }

    SearchScreenBody(
        headerState = headerState,
        uiState = uiState,
        searchHistory = searchHistory,
        gridState = gridState,
        viewModel = viewModel,
        onModelClick = onModelClick,
        gridColumns = gridColumns,
        lazyPagingItems = lazyPagingItems,
        compareModelName = compareModelName,
        onCancelCompare = onCancelCompare,
        ownedHashes = ownedHashes,
        onDiscoverClick = onDiscoverClick,
        favoriteIds = favoriteIds,
        onToggleFavorite = viewModel::toggleFavorite,
        onCompareModel = onCompareModel,
        onScanQRCode = onScanQRCode,
    )
}

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenBody(
    headerState: CollapsibleHeaderState,
    uiState: com.riox432.civitdeck.feature.search.presentation.ModelSearchUiState,
    searchHistory: List<String>,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    viewModel: ModelSearchViewModel,
    onModelClick: (Long, String?, String) -> Unit,
    gridColumns: Int,
    lazyPagingItems: androidx.paging.compose.LazyPagingItems<com.riox432.civitdeck.domain.model.Model>,
    compareModelName: String? = null,
    onCancelCompare: () -> Unit = {},
    ownedHashes: Set<String> = emptySet(),
    onDiscoverClick: () -> Unit = {},
    favoriteIds: Set<Long> = emptySet(),
    onToggleFavorite: (com.riox432.civitdeck.domain.model.Model) -> Unit = {},
    onCompareModel: (Long, String) -> Unit = { _, _ -> },
    onScanQRCode: () -> Unit = {},
) {
    val padding = PaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSavedFiltersSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveFilterName by remember { mutableStateOf("") }
    val savedFilters by viewModel.savedFilters.collectAsStateWithLifecycle()
    val activeFilterCount = countActiveFilters(uiState)
    val isFabVisible by remember {
        derivedStateOf {
            headerState.headerHeightPx <= 0f ||
                headerState.offsetPx > -headerState.headerHeightPx * 0.3f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .padding(
                top = padding.calculateTopPadding(),
                start = padding.calculateLeftPadding(layoutDirection),
                end = padding.calculateRightPadding(layoutDirection),
            )
            .nestedScroll(headerState.nestedScrollConnection),
    ) {
        val visibleHeaderHeightPx =
            (headerState.headerHeightPx + headerState.offsetPx).coerceAtLeast(0f)
        val topPadding = with(density) { visibleHeaderHeightPx.toDp() }

        ModelSearchContent(
            recommendations = uiState.recommendations,
            gridState = gridState,
            lazyPagingItems = lazyPagingItems,
            onModelClick = onModelClick,
            onHideModel = viewModel::onHideModel,
            topPadding = topPadding,
            bottomPadding = padding.calculateBottomPadding(),
            gridColumns = gridColumns,
            ownedHashes = ownedHashes,
            favoriteIds = favoriteIds,
            onToggleFavorite = onToggleFavorite,
            onCompareModel = onCompareModel,
            isComparing = compareModelName != null,
        )

        CollapsibleHeader(
            headerState = headerState,
            query = uiState.query,
            searchHistory = searchHistory,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::onSearch,
            onHistoryItemClick = viewModel::onHistoryItemClick,
            onDeleteHistoryItem = viewModel::removeSearchHistoryItem,
            onClearHistory = viewModel::clearSearchHistory,
        )

        QRScannerFab(
            visible = isFabVisible,
            onClick = onScanQRCode,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = QR_FAB_BOTTOM_PADDING, end = Spacing.lg),
        )

        DiscoverFab(
            visible = isFabVisible,
            onClick = onDiscoverClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = DISCOVER_FAB_BOTTOM_PADDING, end = Spacing.lg),
        )

        FilterFab(
            activeFilterCount = activeFilterCount,
            visible = isFabVisible,
            onClick = { showFilterSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.lg),
        )

        ComparisonBottomBar(
            compareModelName = compareModelName,
            onCancel = onCancelCompare,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            uiState = uiState,
            onDismiss = { showFilterSheet = false },
            onShowSavedFilters = { showSavedFiltersSheet = true },
            onSaveFilter = { showSaveDialog = true },
            onResetFilters = viewModel::resetFilters,
            onTypeSelected = viewModel::onTypeSelected,
            onBaseModelToggled = viewModel::onBaseModelToggled,
            onSortSelected = viewModel::onSortSelected,
            onPeriodSelected = viewModel::onPeriodSelected,
            onFreshFindToggled = viewModel::onFreshFindToggled,
            onQualityFilterToggled = viewModel::onQualityFilterToggled,
            onAddIncludedTag = viewModel::onAddIncludedTag,
            onRemoveIncludedTag = viewModel::onRemoveIncludedTag,
            onAddExcludedTag = viewModel::onAddExcludedTag,
            onRemoveExcludedTag = viewModel::onRemoveExcludedTag,
            onSourceToggled = viewModel::toggleSource,
        )
    }
    if (showSavedFiltersSheet) {
        SavedFiltersSheet(
            savedFilters = savedFilters,
            onApply = { filter ->
                viewModel.applyFilter(filter)
                showFilterSheet = false
            },
            onDelete = viewModel::deleteSavedFilter,
            onDismiss = { showSavedFiltersSheet = false },
        )
    }
    if (showSaveDialog) {
        SaveFilterDialog(
            filterName = saveFilterName,
            onFilterNameChange = { saveFilterName = it },
            onConfirm = {
                if (saveFilterName.isNotBlank()) {
                    viewModel.saveCurrentFilter(saveFilterName.trim())
                }
                showSaveDialog = false
                saveFilterName = ""
            },
            onDismiss = {
                showSaveDialog = false
                saveFilterName = ""
            },
        )
    }
}

@Composable
private fun SaveFilterDialog(
    filterName: String,
    onFilterNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Filter") },
        text = {
            OutlinedTextField(
                value = filterName,
                onValueChange = onFilterNameChange,
                label = { Text("Filter name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DiscoverFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(Duration.fast, easing = Easing.standard),
        ) + fadeIn(animationSpec = tween(Duration.fast, easing = Easing.standard)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(Duration.fast, easing = Easing.standard),
        ) + fadeOut(animationSpec = tween(Duration.fast, easing = Easing.standard)),
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                Icons.Filled.Style,
                contentDescription = stringResource(R.string.cd_discover),
            )
        }
    }
}

@Composable
private fun QRScannerFab(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(Duration.fast, easing = Easing.standard),
        ) + fadeIn(animationSpec = tween(Duration.fast, easing = Easing.standard)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(Duration.fast, easing = Easing.standard),
        ) + fadeOut(animationSpec = tween(Duration.fast, easing = Easing.standard)),
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ) {
            Icon(
                Icons.Filled.QrCodeScanner,
                contentDescription = stringResource(R.string.cd_scan_qr_code),
            )
        }
    }
}

@Composable
private fun FilterFab(
    activeFilterCount: Int,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(Duration.fast, easing = Easing.standard),
        ) + fadeIn(animationSpec = tween(Duration.fast, easing = Easing.standard)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(Duration.fast, easing = Easing.standard),
        ) + fadeOut(animationSpec = tween(Duration.fast, easing = Easing.standard)),
    ) {
        BadgedBox(
            badge = {
                if (activeFilterCount > 0) {
                    Badge { Text(activeFilterCount.toString()) }
                }
            },
        ) {
            FloatingActionButton(
                onClick = onClick,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Outlined.FilterList, contentDescription = stringResource(R.string.cd_filters))
            }
        }
    }
}

private val DISCOVER_FAB_BOTTOM_PADDING = 80.dp
private val QR_FAB_BOTTOM_PADDING = 136.dp
