package com.riox432.civitdeck.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.BuildConfig
import com.riox432.civitdeck.R
import com.riox432.civitdeck.feature.search.presentation.ModelSearchViewModel
import com.riox432.civitdeck.ui.adaptive.adaptiveGridColumns
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.Spacing

/**
 * Groups navigation/action callback parameters for the search screen.
 */
data class SearchScreenCallbacks(
    val onModelClick: (Long, String?, String) -> Unit = { _, _, _ -> },
    val onCancelCompare: () -> Unit = {},
    val onDiscoverClick: () -> Unit = {},
    val onCompareModel: (Long, String) -> Unit = { _, _ -> },
    val onScanQRCode: () -> Unit = {},
    val onTextSearch: () -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSearchScreen(
    viewModel: ModelSearchViewModel,
    callbacks: SearchScreenCallbacks = SearchScreenCallbacks(),
    scrollToTopTrigger: Int = 0,
    compareModelName: String? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val userGridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val gridColumns = adaptiveGridColumns(userGridColumns)
    val ownedHashes by viewModel.ownedHashes.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
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
        gridColumns = gridColumns,
        compareModelName = compareModelName,
        ownedHashes = ownedHashes,
        favoriteIds = favoriteIds,
        callbacks = callbacks,
        onToggleFavorite = viewModel::toggleFavorite,
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
    gridColumns: Int,
    compareModelName: String? = null,
    ownedHashes: Set<String> = emptySet(),
    favoriteIds: Set<Long> = emptySet(),
    callbacks: SearchScreenCallbacks,
    onToggleFavorite: (com.riox432.civitdeck.domain.model.Model) -> Unit = {},
) {
    val padding = WindowInsets.safeDrawing.asPaddingValues()
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
            models = uiState.models,
            isLoading = uiState.isLoading,
            isLoadingMore = uiState.isLoadingMore,
            error = uiState.error,
            onLoadMore = viewModel::loadMore,
            onRefresh = viewModel::refresh,
            callbacks = ModelGridCallbacks(
                onModelClick = callbacks.onModelClick,
                onHideModel = viewModel::onHideModel,
                onToggleFavorite = onToggleFavorite,
                onCompareModel = callbacks.onCompareModel,
            ),
            topPadding = topPadding,
            bottomPadding = padding.calculateBottomPadding(),
            gridColumns = gridColumns,
            ownedHashes = ownedHashes,
            favoriteIds = favoriteIds,
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

        SpeedDialFab(
            visible = isFabVisible,
            activeFilterCount = activeFilterCount,
            onFilterClick = { showFilterSheet = true },
            onDiscoverClick = callbacks.onDiscoverClick,
            onScanQRCode = callbacks.onScanQRCode,
            onTextSearch = if (BuildConfig.FEATURE_SIMILARITY_SEARCH) callbacks.onTextSearch else null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.lg),
        )

        ComparisonBottomBar(
            compareModelName = compareModelName,
            onCancel = callbacks.onCancelCompare,
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
            filterCallbacks = FilterCallbacks(
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
            ),
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

@Suppress("LongParameterList")
@Composable
private fun SpeedDialFab(
    visible: Boolean,
    activeFilterCount: Int,
    onFilterClick: () -> Unit,
    onDiscoverClick: () -> Unit,
    onScanQRCode: () -> Unit,
    onTextSearch: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

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
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Expandable mini-FABs
            SpeedDialItems(
                expanded = expanded,
                onFilterClick = {
                    expanded = false
                    onFilterClick()
                },
                activeFilterCount = activeFilterCount,
                onDiscoverClick = {
                    expanded = false
                    onDiscoverClick()
                },
                onScanQRCode = {
                    expanded = false
                    onScanQRCode()
                },
                onTextSearch = onTextSearch?.let { {
                    expanded = false
                    it()
                }
                },
            )
            PrimarySpeedDialFab(
                expanded = expanded,
                activeFilterCount = activeFilterCount,
                onToggle = { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun PrimarySpeedDialFab(
    expanded: Boolean,
    activeFilterCount: Int,
    onToggle: () -> Unit,
) {
    BadgedBox(
        badge = {
            if (activeFilterCount > 0 && !expanded) {
                Badge { Text(activeFilterCount.toString()) }
            }
        },
    ) {
        FloatingActionButton(
            onClick = onToggle,
            containerColor = if (expanded) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (expanded) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            },
        ) {
            Icon(
                if (expanded) Icons.Default.Close else Icons.Outlined.FilterList,
                contentDescription = stringResource(R.string.cd_filters),
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun SpeedDialItems(
    expanded: Boolean,
    onFilterClick: () -> Unit,
    activeFilterCount: Int,
    onDiscoverClick: () -> Unit,
    onScanQRCode: () -> Unit,
    onTextSearch: (() -> Unit)?,
) {
    val enterAnim = scaleIn(animationSpec = tween(Duration.fast, easing = Easing.standard)) +
        fadeIn(animationSpec = tween(Duration.fast, easing = Easing.standard))
    val exitAnim = scaleOut(animationSpec = tween(Duration.fast, easing = Easing.standard)) +
        fadeOut(animationSpec = tween(Duration.fast, easing = Easing.standard))

    if (onTextSearch != null) {
        SpeedDialItem(
            visible = expanded,
            label = "AI Search",
            icon = { Icon(Icons.Outlined.AutoAwesome, contentDescription = "AI Search") },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            onClick = onTextSearch,
            enter = enterAnim,
            exit = exitAnim,
        )
    }
    SpeedDialItem(
        visible = expanded,
        label = stringResource(R.string.cd_scan_qr_code),
        icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = stringResource(R.string.cd_scan_qr_code)) },
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        onClick = onScanQRCode,
        enter = enterAnim,
        exit = exitAnim,
    )
    SpeedDialItem(
        visible = expanded,
        label = stringResource(R.string.cd_discover),
        icon = { Icon(Icons.Filled.Style, contentDescription = stringResource(R.string.cd_discover)) },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = onDiscoverClick,
        enter = enterAnim,
        exit = exitAnim,
    )
    SpeedDialItem(
        visible = expanded,
        label = stringResource(R.string.cd_filters) +
            if (activeFilterCount > 0) " ($activeFilterCount)" else "",
        icon = { Icon(Icons.Outlined.FilterList, contentDescription = stringResource(R.string.cd_filters)) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onFilterClick,
        enter = enterAnim,
        exit = exitAnim,
    )
}

@Composable
private fun SpeedDialItem(
    visible: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    enter: androidx.compose.animation.EnterTransition,
    exit: androidx.compose.animation.ExitTransition,
) {
    AnimatedVisibility(visible = visible, enter = enter, exit = exit) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SmallFloatingActionButton(
                onClick = onClick,
                containerColor = containerColor,
            ) {
                icon()
            }
        }
    }
}
