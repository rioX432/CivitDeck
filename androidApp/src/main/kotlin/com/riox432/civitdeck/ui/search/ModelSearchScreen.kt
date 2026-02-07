package com.riox432.civitdeck.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.ui.components.ModelCard
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSearchScreen(
    viewModel: ModelSearchViewModel,
    onModelClick: (Long, String?) -> Unit = { _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

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

    Scaffold(
        topBar = { TopAppBar(title = { Text("CivitDeck") }) },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = padding.calculateTopPadding(),
                    start = padding.calculateLeftPadding(layoutDirection),
                    end = padding.calculateRightPadding(layoutDirection),
                ),
        ) {
            SearchBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = viewModel::onSearch,
            )
            TypeFilterChips(
                selectedType = uiState.selectedType,
                onTypeSelected = viewModel::onTypeSelected,
            )
            SortAndPeriodChips(
                selectedSort = uiState.selectedSort,
                selectedPeriod = uiState.selectedPeriod,
                onSortSelected = viewModel::onSortSelected,
                onPeriodSelected = viewModel::onPeriodSelected,
            )
            ModelSearchContent(
                uiState = uiState,
                gridState = gridState,
                onRefresh = viewModel::refresh,
                onModelClick = onModelClick,
                bottomPadding = padding.calculateBottomPadding(),
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        placeholder = { Text("Search models...") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
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
            },
        ),
    )
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
private fun SortAndPeriodChips(
    selectedSort: SortOrder,
    selectedPeriod: TimePeriod,
    onSortSelected: (SortOrder) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
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
    onModelClick: (Long, String?) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val stateKey = when {
        uiState.isLoading && uiState.models.isEmpty() -> "loading"
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
                        gridState = gridState,
                        isLoadingMore = uiState.isLoadingMore,
                        onModelClick = onModelClick,
                        bottomPadding = bottomPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelGrid(
    models: List<Model>,
    gridState: LazyGridState,
    isLoadingMore: Boolean,
    onModelClick: (Long, String?) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.sm,
            bottom = Spacing.lg + bottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = models, key = { it.id }) { model ->
            val thumbnailUrl = model.modelVersions
                .firstOrNull()?.images?.firstOrNull()?.url
            ModelCard(
                model = model,
                onClick = { onModelClick(model.id, thumbnailUrl) },
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
