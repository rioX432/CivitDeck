package com.riox432.civitdeck.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.RecommendationSection
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.model.thumbnailUrl
import com.riox432.civitdeck.ui.components.ModelCard
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.Spacing
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

private class CollapsibleHeaderState {
    var headerHeightPx by mutableFloatStateOf(0f)
    var offsetPx by mutableFloatStateOf(0f)
    val animatable = Animatable(0f)

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (headerHeightPx <= 0f) return Offset.Zero
            val newOffset = (offsetPx + available.y).coerceIn(-headerHeightPx, 0f)
            val consumed = newOffset - offsetPx
            offsetPx = newOffset
            return Offset(0f, consumed)
        }
    }
}

@Composable
private fun rememberCollapsibleHeaderState(): CollapsibleHeaderState {
    return remember { CollapsibleHeaderState() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSearchScreen(
    viewModel: ModelSearchViewModel,
    onModelClick: (Long, String?, String) -> Unit = { _, _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.pagingData.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    val headerState = rememberCollapsibleHeaderState()

    HeaderSnapEffect(gridState = gridState, headerState = headerState)

    SearchScreenBody(
        padding = PaddingValues(),
        headerState = headerState,
        uiState = uiState,
        searchHistory = searchHistory,
        gridState = gridState,
        viewModel = viewModel,
        onModelClick = onModelClick,
        gridColumns = gridColumns,
        lazyPagingItems = lazyPagingItems,
    )
}

@Composable
private fun HeaderSnapEffect(gridState: LazyGridState, headerState: CollapsibleHeaderState) {
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .drop(1)
            .collectLatest { scrolling ->
                if (!scrolling && headerState.headerHeightPx > 0f) {
                    val threshold = -headerState.headerHeightPx / 2f
                    val target = if (headerState.offsetPx < threshold) {
                        -headerState.headerHeightPx
                    } else {
                        0f
                    }
                    headerState.animatable.snapTo(headerState.offsetPx)
                    headerState.animatable.animateTo(
                        targetValue = target,
                        animationSpec = tween(
                            durationMillis = Duration.fast,
                            easing = Easing.standard,
                        ),
                    ) {
                        headerState.offsetPx = value
                    }
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenBody(
    padding: PaddingValues,
    headerState: CollapsibleHeaderState,
    uiState: ModelSearchUiState,
    searchHistory: List<String>,
    gridState: LazyGridState,
    viewModel: ModelSearchViewModel,
    onModelClick: (Long, String?, String) -> Unit,
    gridColumns: Int,
    lazyPagingItems: LazyPagingItems<Model>,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    var showFilterSheet by remember { mutableStateOf(false) }
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
            uiState = uiState,
            gridState = gridState,
            lazyPagingItems = lazyPagingItems,
            onModelClick = onModelClick,
            onHideModel = viewModel::onHideModel,
            topPadding = topPadding,
            bottomPadding = padding.calculateBottomPadding(),
            gridColumns = gridColumns,
        )

        CollapsibleHeader(
            headerState = headerState,
            uiState = uiState,
            searchHistory = searchHistory,
            viewModel = viewModel,
        )

        FilterFab(
            activeFilterCount = activeFilterCount,
            visible = isFabVisible,
            onClick = { showFilterSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.lg),
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showFilterSheet = false },
        )
    }
}

@Composable
private fun CollapsibleHeader(
    headerState: CollapsibleHeaderState,
    uiState: ModelSearchUiState,
    searchHistory: List<String>,
    viewModel: ModelSearchViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .graphicsLayer { translationY = headerState.offsetPx }
            .onGloballyPositioned { coordinates ->
                headerState.headerHeightPx = coordinates.size.height.toFloat()
            }
            .background(MaterialTheme.colorScheme.surface),
    ) {
        SearchBarWithFilterButton(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::onSearch,
            searchHistory = searchHistory,
            onHistoryItemClick = viewModel::onHistoryItemClick,
            onClearHistory = viewModel::clearSearchHistory,
        )
    }
}

private fun countActiveFilters(uiState: ModelSearchUiState): Int {
    var count = 0
    if (uiState.selectedType != null) count++
    if (uiState.selectedBaseModels.isNotEmpty()) count++
    if (uiState.selectedSort != SortOrder.MostDownloaded) count++
    if (uiState.selectedPeriod != TimePeriod.AllTime) count++
    if (uiState.isFreshFindEnabled) count++
    if (uiState.includedTags.isNotEmpty()) count++
    if (uiState.excludedTags.isNotEmpty()) count++
    return count
}

@Composable
private fun SearchBarWithFilterButton(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searchHistory: List<String>,
    onHistoryItemClick: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var showHistory by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        SearchTextField(
            query = query,
            onQueryChange = {
                onQueryChange(it)
                showHistory = it.isEmpty() && searchHistory.isNotEmpty()
            },
            onSearch = {
                onSearch()
                keyboardController?.hide()
                showHistory = false
            },
            onFocusChanged = { focused ->
                showHistory = focused && query.isEmpty() && searchHistory.isNotEmpty()
            },
            onClear = {
                onQueryChange("")
                showHistory = searchHistory.isNotEmpty()
            },
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(visible = showHistory) {
            SearchHistoryDropdown(
                history = searchHistory,
                onItemClick = { item ->
                    onHistoryItemClick(item)
                    showHistory = false
                    keyboardController?.hide()
                },
                onClearAll = {
                    onClearHistory()
                    showHistory = false
                },
            )
        }
    }
}

@Composable
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.onFocusChanged { onFocusChanged(it.isFocused) },
        placeholder = { Text("Search models...") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
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
                Icon(Icons.Outlined.FilterList, contentDescription = "Filters")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    uiState: ModelSearchUiState,
    viewModel: ModelSearchViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        FilterSheetContent(uiState = uiState, viewModel = viewModel, onDismiss = onDismiss)
    }
}

@Suppress("LongMethod")
@Composable
private fun FilterSheetContent(
    uiState: ModelSearchUiState,
    viewModel: ModelSearchViewModel,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight(0.95f)
            .verticalScroll(rememberScrollState())
            .padding(bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Filters", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = {
                viewModel.resetFilters()
                onDismiss()
            }) {
                Text("Reset")
            }
        }
        TypeFilterSection(
            selectedType = uiState.selectedType,
            onTypeSelected = viewModel::onTypeSelected,
        )
        BaseModelFilterSection(
            selectedBaseModels = uiState.selectedBaseModels,
            onBaseModelToggled = viewModel::onBaseModelToggled,
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
        SortFilterSection(
            selectedSort = uiState.selectedSort,
            onSortSelected = viewModel::onSortSelected,
        )
        PeriodFilterSection(
            selectedPeriod = uiState.selectedPeriod,
            onPeriodSelected = viewModel::onPeriodSelected,
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg))
        FreshOnlyToggleRow(
            isFreshFindEnabled = uiState.isFreshFindEnabled,
            onFreshFindToggled = viewModel::onFreshFindToggled,
        )
        TagFilterSection(
            tags = uiState.includedTags,
            onAddTag = viewModel::onAddIncludedTag,
            onRemoveTag = viewModel::onRemoveIncludedTag,
            placeholder = "Include tag...",
            header = "Tags",
            headerSubtitle = "(include)",
            chipBackground = { MaterialTheme.colorScheme.primaryContainer },
            chipForeground = { MaterialTheme.colorScheme.onPrimaryContainer },
        )
        TagFilterSection(
            tags = uiState.excludedTags,
            onAddTag = viewModel::onAddExcludedTag,
            onRemoveTag = viewModel::onRemoveExcludedTag,
            placeholder = "Exclude tag...",
            chipBackground = { MaterialTheme.colorScheme.errorContainer },
            chipForeground = { MaterialTheme.colorScheme.onErrorContainer },
        )
    }
}

@Composable
private fun SearchHistoryDropdown(
    history: List<String>,
    onItemClick: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(bottomStart = CornerRadius.card, bottomEnd = CornerRadius.card),
            )
            .padding(vertical = Spacing.xs),
    ) {
        history.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) }
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = null,
                    modifier = Modifier.padding(end = Spacing.sm),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = "Clear history",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onClearAll)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
    }
}

private val filterTypes = listOf(null) + listOf(
    ModelType.Checkpoint,
    ModelType.LORA,
    ModelType.LoCon,
    ModelType.Controlnet,
    ModelType.TextualInversion,
    ModelType.Hypernetwork,
    ModelType.Upscaler,
    ModelType.VAE,
    ModelType.Poses,
    ModelType.Wildcards,
    ModelType.Workflows,
    ModelType.MotionModule,
    ModelType.AestheticGradient,
    ModelType.Other,
)

private fun ModelType.displayLabel(): String = when (this) {
    ModelType.TextualInversion -> "Textual Inv."
    ModelType.AestheticGradient -> "Aesthetic Grad."
    ModelType.MotionModule -> "Motion Module"
    else -> name
}

private fun SortOrder.displayLabel(): String = when (this) {
    SortOrder.HighestRated -> "Highest Rated"
    SortOrder.MostDownloaded -> "Most Downloaded"
    else -> name
}

private fun TimePeriod.displayLabel(): String = when (this) {
    TimePeriod.AllTime -> "All Time"
    else -> name
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypeFilterSection(
    selectedType: ModelType?,
    onTypeSelected: (ModelType?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilterSectionHeader("Type")
        FlowRow(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            filterTypes.forEach { type ->
                FilterChipItem(
                    label = type?.displayLabel() ?: "All",
                    isSelected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BaseModelFilterSection(
    selectedBaseModels: Set<BaseModel>,
    onBaseModelToggled: (BaseModel) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilterSectionHeader("Base Model", "(multiple)")
        FlowRow(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            BaseModel.entries.forEach { baseModel ->
                FilterChipItem(
                    label = baseModel.displayName,
                    isSelected = baseModel in selectedBaseModels,
                    onClick = { onBaseModelToggled(baseModel) },
                    showCheckmark = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SortFilterSection(
    selectedSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilterSectionHeader("Sort")
        FlowRow(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SortOrder.entries.forEach { sort ->
                FilterChipItem(
                    label = sort.displayLabel(),
                    isSelected = selectedSort == sort,
                    onClick = { onSortSelected(sort) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodFilterSection(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        FilterSectionHeader("Period")
        FlowRow(
            modifier = Modifier.padding(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TimePeriod.entries.forEach { period ->
                FilterChipItem(
                    label = period.displayLabel(),
                    isSelected = selectedPeriod == period,
                    onClick = { onPeriodSelected(period) },
                )
            }
        }
    }
}

@Composable
private fun FreshOnlyToggleRow(
    isFreshFindEnabled: Boolean,
    onFreshFindToggled: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Fresh Only", style = MaterialTheme.typography.titleSmall)
        Switch(checked = isFreshFindEnabled, onCheckedChange = { onFreshFindToggled() })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagFilterSection(
    tags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    placeholder: String,
    header: String? = null,
    headerSubtitle: String? = null,
    chipBackground: @Composable () -> androidx.compose.ui.graphics.Color,
    chipForeground: @Composable () -> androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (header != null) {
            FilterSectionHeader(header, headerSubtitle)
        }
        TagInputRow(placeholder = placeholder, onAddTag = onAddTag)
        if (tags.isNotEmpty()) {
            val bg = chipBackground()
            val fg = chipForeground()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                tags.forEach { tag ->
                    TagChip(tag = tag, onRemove = { onRemoveTag(tag) }, background = bg, foreground = fg)
                }
            }
        }
    }
}

@Composable
private fun TagInputRow(placeholder: String, onAddTag: (String) -> Unit) {
    var tagInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = tagInput,
            onValueChange = { tagInput = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text(placeholder) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (tagInput.isNotBlank()) {
                        onAddTag(tagInput)
                        tagInput = ""
                        keyboardController?.hide()
                    }
                },
            ),
        )
        IconButton(
            onClick = {
                if (tagInput.isNotBlank()) {
                    onAddTag(tagInput)
                    tagInput = ""
                    keyboardController?.hide()
                }
            },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add tag")
        }
    }
}

@Composable
private fun TagChip(
    tag: String,
    onRemove: () -> Unit,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(CornerRadius.chip))
            .padding(start = Spacing.sm, end = Spacing.xs)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(text = tag, style = MaterialTheme.typography.labelSmall, color = foreground)
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove $tag",
            modifier = Modifier.size(14.dp).clickable(onClick = onRemove),
            tint = foreground,
        )
    }
}

@Composable
private fun FilterSectionHeader(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    showCheckmark: Boolean = false,
) {
    val colorTween = tween<androidx.compose.ui.graphics.Color>(
        durationMillis = Duration.fast,
        easing = Easing.standard,
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = colorTween,
        label = "chipBg",
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = colorTween,
        label = "chipText",
    )
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(RoundedCornerShape(CornerRadius.chip))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        if (showCheckmark && isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textColor,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) {
                androidx.compose.ui.text.font.FontWeight.SemiBold
            } else {
                androidx.compose.ui.text.font.FontWeight.Normal
            },
            color = textColor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSearchContent(
    uiState: ModelSearchUiState,
    gridState: LazyGridState,
    lazyPagingItems: LazyPagingItems<Model>,
    onModelClick: (Long, String?, String) -> Unit,
    onHideModel: (Long, String) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    gridColumns: Int = 2,
) {
    val refreshState = lazyPagingItems.loadState.refresh
    val isInitialLoading = refreshState is LoadState.Loading ||
        (lazyPagingItems.itemCount == 0 && uiState.isLoadingRecommendations)
    val refreshError = (refreshState as? LoadState.Error)?.error
    val stateKey = when {
        isInitialLoading -> "loading"
        refreshError != null && lazyPagingItems.itemCount == 0 -> "error"
        else -> "content"
    }

    PullToRefreshBox(
        isRefreshing = refreshState is LoadState.Loading && lazyPagingItems.itemCount > 0,
        onRefresh = { lazyPagingItems.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        androidx.compose.animation.Crossfade(
            targetState = stateKey,
            animationSpec = tween(
                durationMillis = Duration.normal,
                easing = Easing.standard,
            ),
            label = "searchContent",
        ) { state ->
            when (state) {
                "loading" -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                "error" -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = refreshError?.message ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                else -> {
                    ModelGrid(
                        lazyPagingItems = lazyPagingItems,
                        recommendations = uiState.recommendations,
                        gridState = gridState,
                        onModelClick = onModelClick,
                        onHideModel = onHideModel,
                        topPadding = topPadding,
                        bottomPadding = bottomPadding,
                        gridColumns = gridColumns,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModelGrid(
    lazyPagingItems: LazyPagingItems<Model>,
    recommendations: List<RecommendationSection>,
    gridState: LazyGridState,
    onModelClick: (Long, String?, String) -> Unit,
    onHideModel: (Long, String) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    gridColumns: Int = 2,
) {
    val isAppendLoading = lazyPagingItems.loadState.append is LoadState.Loading

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        state = gridState,
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.sm + topPadding,
            bottom = Spacing.lg + bottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        recommendations.forEachIndexed { index, section ->
            item(
                key = "rec_${section.title}",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                RecommendationRow(
                    section = section,
                    sharedElementSuffix = "rec$index",
                    onModelClick = onModelClick,
                )
            }
        }
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it.id },
        ) { index ->
            val model = lazyPagingItems[index] ?: return@items
            val thumbnailUrl = model.modelVersions
                .firstOrNull()?.images?.firstOrNull()?.thumbnailUrl()
            ModelCardWithContextMenu(
                model = model,
                onClick = { onModelClick(model.id, thumbnailUrl, "") },
                onHide = { onHideModel(model.id, model.name) },
                modifier = Modifier.animateItem(),
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            AnimatedVisibility(
                visible = isAppendLoading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModelCardWithContextMenu(
    model: Model,
    onClick: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = { showMenu = true },
        ),
    ) {
        ModelCard(model = model)
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Hide model") },
                onClick = {
                    showMenu = false
                    onHide()
                },
            )
        }
    }
}

@Composable
private fun RecommendationRow(
    section: RecommendationSection,
    sharedElementSuffix: String,
    onModelClick: (Long, String?, String) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = Spacing.sm)) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs),
        )
        Text(
            text = section.reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xs, end = Spacing.xs, bottom = Spacing.sm),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(items = section.models, key = { it.id }) { model ->
                val thumbnailUrl = model.modelVersions
                    .firstOrNull()?.images?.firstOrNull()?.url
                ModelCard(
                    model = model,
                    onClick = { onModelClick(model.id, thumbnailUrl, sharedElementSuffix) },
                    modifier = Modifier
                        .width(160.dp),
                    sharedElementSuffix = sharedElementSuffix,
                )
            }
        }
    }
}
