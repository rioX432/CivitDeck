package com.riox432.civitdeck.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.RecommendationSection
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
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
    onSavedPromptsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val headerState = rememberCollapsibleHeaderState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    HeaderSnapEffect(gridState = gridState, headerState = headerState)

    Scaffold(
        topBar = {
            SearchTopBar(
                onSavedPromptsClick = onSavedPromptsClick,
                onSettingsClick = onSettingsClick,
            )
        },
    ) { padding ->
        SearchScreenBody(
            padding = padding,
            headerState = headerState,
            uiState = uiState,
            searchHistory = searchHistory,
            gridState = gridState,
            viewModel = viewModel,
            onModelClick = onModelClick,
        )
    }
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

@Composable
private fun SearchScreenBody(
    padding: PaddingValues,
    headerState: CollapsibleHeaderState,
    uiState: ModelSearchUiState,
    searchHistory: List<String>,
    gridState: LazyGridState,
    viewModel: ModelSearchViewModel,
    onModelClick: (Long, String?, String) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxSize()
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
            onRefresh = viewModel::refresh,
            onModelClick = onModelClick,
            onHideModel = viewModel::onHideModel,
            topPadding = topPadding,
            bottomPadding = padding.calculateBottomPadding(),
        )

        CollapsibleHeader(
            headerState = headerState,
            uiState = uiState,
            searchHistory = searchHistory,
            viewModel = viewModel,
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
        SearchBarWithHistory(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::onSearch,
            searchHistory = searchHistory,
            onHistoryItemClick = viewModel::onHistoryItemClick,
            onClearHistory = viewModel::clearSearchHistory,
        )
        SearchFilters(
            uiState = uiState,
            onTypeSelected = viewModel::onTypeSelected,
            onBaseModelToggled = viewModel::onBaseModelToggled,
            onSortSelected = viewModel::onSortSelected,
            onPeriodSelected = viewModel::onPeriodSelected,
            onFreshFindToggled = viewModel::onFreshFindToggled,
            onAddExcludedTag = viewModel::onAddExcludedTag,
            onRemoveExcludedTag = viewModel::onRemoveExcludedTag,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    onSavedPromptsClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    TopAppBar(
        title = { Text("CivitDeck") },
        actions = {
            IconButton(onClick = onSavedPromptsClick) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Saved Prompts")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
            }
        },
    )
}

@Composable
private fun SearchFilters(
    uiState: ModelSearchUiState,
    onTypeSelected: (ModelType?) -> Unit,
    onBaseModelToggled: (BaseModel) -> Unit,
    onSortSelected: (SortOrder) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
    onFreshFindToggled: () -> Unit,
    onAddExcludedTag: (String) -> Unit,
    onRemoveExcludedTag: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        TypeFilterChips(selectedType = uiState.selectedType, onTypeSelected = onTypeSelected)
        BaseModelFilterChips(selectedBaseModels = uiState.selectedBaseModels, onBaseModelToggled = onBaseModelToggled)
        SortAndPeriodFilters(
            selectedSort = uiState.selectedSort,
            selectedPeriod = uiState.selectedPeriod,
            isFreshFindEnabled = uiState.isFreshFindEnabled,
            onSortSelected = onSortSelected,
            onPeriodSelected = onPeriodSelected,
            onFreshFindToggled = onFreshFindToggled,
        )
        ExcludedTagsSection(
            excludedTags = uiState.excludedTags,
            onAddTag = onAddExcludedTag,
            onRemoveTag = onRemoveExcludedTag,
        )
    }
}

@Composable
private fun SearchBarWithHistory(
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
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                showHistory = it.isEmpty() && searchHistory.isNotEmpty()
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    showHistory = focusState.isFocused &&
                        query.isEmpty() && searchHistory.isNotEmpty()
                },
            placeholder = { Text("Search models...") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        onQueryChange("")
                        showHistory = searchHistory.isNotEmpty()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch()
                    keyboardController?.hide()
                    showHistory = false
                },
            ),
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

@Composable
private fun TypeFilterChips(
    selectedType: ModelType?,
    onTypeSelected: (ModelType?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(filterTypes) { type ->
            FilterChipItem(
                label = type?.name ?: "All",
                isSelected = selectedType == type,
                onClick = { onTypeSelected(type) },
            )
        }
    }
}

@Composable
private fun BaseModelFilterChips(
    selectedBaseModels: Set<BaseModel>,
    onBaseModelToggled: (BaseModel) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(BaseModel.entries.toList()) { baseModel ->
            FilterChipItem(
                label = baseModel.displayName,
                isSelected = baseModel in selectedBaseModels,
                onClick = { onBaseModelToggled(baseModel) },
            )
        }
    }
}

@Composable
private fun SortAndPeriodFilters(
    selectedSort: SortOrder,
    selectedPeriod: TimePeriod,
    isFreshFindEnabled: Boolean,
    onSortSelected: (SortOrder) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
    onFreshFindToggled: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            FilterChipItem(
                label = "Fresh Only",
                isSelected = isFreshFindEnabled,
                onClick = onFreshFindToggled,
            )
        }
        items(SortOrder.entries.toList()) { sort ->
            FilterChipItem(
                label = sort.name,
                isSelected = selectedSort == sort,
                onClick = { onSortSelected(sort) },
            )
        }
        items(TimePeriod.entries.toList()) { period ->
            FilterChipItem(
                label = period.name,
                isSelected = selectedPeriod == period,
                onClick = { onPeriodSelected(period) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExcludedTagsSection(
    excludedTags: List<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
) {
    var tagInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier.padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Exclude tag...") },
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
        if (excludedTags.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                excludedTags.forEach { tag ->
                    ExcludedTagChip(tag = tag, onRemove = { onRemoveTag(tag) })
                }
            }
        }
    }
}

@Composable
private fun ExcludedTagChip(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.errorContainer,
                RoundedCornerShape(CornerRadius.chip),
            )
            .padding(start = Spacing.sm, end = Spacing.xs)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove $tag",
            modifier = Modifier
                .size(14.dp)
                .clickable(onClick = onRemove),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
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
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (isSelected) {
            androidx.compose.ui.text.font.FontWeight.SemiBold
        } else {
            androidx.compose.ui.text.font.FontWeight.Normal
        },
        color = textColor,
        modifier = Modifier
            .clip(RoundedCornerShape(CornerRadius.chip))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = 6.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSearchContent(
    uiState: ModelSearchUiState,
    gridState: LazyGridState,
    onRefresh: () -> Unit,
    onModelClick: (Long, String?, String) -> Unit,
    onHideModel: (Long, String) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val isInitialLoading = uiState.models.isEmpty() &&
        (uiState.isLoading || uiState.isLoadingRecommendations)
    val stateKey = when {
        isInitialLoading -> "loading"
        uiState.error != null && uiState.models.isEmpty() -> "error"
        else -> "content"
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
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
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                else -> {
                    ModelGrid(
                        models = uiState.models,
                        recommendations = uiState.recommendations,
                        gridState = gridState,
                        isLoadingMore = uiState.isLoadingMore,
                        onModelClick = onModelClick,
                        onHideModel = onHideModel,
                        topPadding = topPadding,
                        bottomPadding = bottomPadding,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModelGrid(
    models: List<Model>,
    recommendations: List<RecommendationSection>,
    gridState: LazyGridState,
    isLoadingMore: Boolean,
    onModelClick: (Long, String?, String) -> Unit,
    onHideModel: (Long, String) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
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
                span = { GridItemSpan(2) },
            ) {
                RecommendationRow(
                    section = section,
                    sharedElementSuffix = "rec$index",
                    onModelClick = onModelClick,
                )
            }
        }
        items(items = models, key = { it.id }) { model ->
            val thumbnailUrl = model.modelVersions
                .firstOrNull()?.images?.firstOrNull()?.url
            ModelCardWithContextMenu(
                model = model,
                onClick = { onModelClick(model.id, thumbnailUrl, "") },
                onHide = { onHideModel(model.id, model.name) },
                modifier = Modifier.animateItem(),
            )
        }
        item(span = { GridItemSpan(2) }) {
            AnimatedVisibility(
                visible = isLoadingMore,
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

    Box(modifier = modifier) {
        ModelCard(
            model = model,
            onClick = onClick,
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true },
            ),
        )
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
