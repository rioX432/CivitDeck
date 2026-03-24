@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.RecommendationSection
import com.riox432.civitdeck.domain.model.thumbnailUrl
import com.riox432.civitdeck.ui.adaptive.isExpandedWidth
import com.riox432.civitdeck.ui.components.LaunchStaggerAnimation
import com.riox432.civitdeck.ui.components.ModelCard
import com.riox432.civitdeck.ui.components.SwipeableModelCard
import com.riox432.civitdeck.ui.components.isReducedMotionEnabled
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.Spacing

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelSearchContent(
    recommendations: List<RecommendationSection>,
    gridState: LazyGridState,
    lazyPagingItems: LazyPagingItems<Model>,
    onModelClick: (Long, String?, String) -> Unit,
    onHideModel: (Long, String) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    gridColumns: Int = 2,
    ownedHashes: Set<String> = emptySet(),
    favoriteIds: Set<Long> = emptySet(),
    onToggleFavorite: (Model) -> Unit = {},
    onCompareModel: (Long, String) -> Unit = { _, _ -> },
    isComparing: Boolean = false,
) {
    val refreshState = lazyPagingItems.loadState.refresh
    val isInitialLoading = refreshState is LoadState.Loading
    val refreshError = (refreshState as? LoadState.Error)?.error
    val stateKey = when {
        isInitialLoading -> "loading"
        refreshError != null && lazyPagingItems.itemCount == 0 -> "error"
        else -> "content"
    }

    SearchContentPullToRefresh(
        refreshState = refreshState,
        lazyPagingItems = lazyPagingItems,
        topPadding = topPadding,
        stateKey = stateKey,
        refreshError = refreshError,
        recommendations = recommendations,
        gridState = gridState,
        onModelClick = onModelClick,
        onHideModel = onHideModel,
        bottomPadding = bottomPadding,
        gridColumns = gridColumns,
        ownedHashes = ownedHashes,
        favoriteIds = favoriteIds,
        onToggleFavorite = onToggleFavorite,
        onCompareModel = onCompareModel,
        isComparing = isComparing,
    )
}

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchContentPullToRefresh(
    refreshState: LoadState,
    lazyPagingItems: LazyPagingItems<Model>,
    topPadding: androidx.compose.ui.unit.Dp,
    stateKey: String,
    refreshError: Throwable?,
    recommendations: List<RecommendationSection>,
    gridState: LazyGridState,
    onModelClick: (Long, String?, String) -> Unit,
    onHideModel: (Long, String) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    gridColumns: Int,
    ownedHashes: Set<String>,
    favoriteIds: Set<Long>,
    onToggleFavorite: (Model) -> Unit,
    onCompareModel: (Long, String) -> Unit,
    isComparing: Boolean,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = refreshState is LoadState.Loading && lazyPagingItems.itemCount > 0,
        onRefresh = { lazyPagingItems.refresh() },
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = refreshState is LoadState.Loading && lazyPagingItems.itemCount > 0,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topPadding),
            )
        },
    ) {
        SearchContentCrossfade(
            stateKey = stateKey,
            refreshError = refreshError,
            lazyPagingItems = lazyPagingItems,
            recommendations = recommendations,
            gridState = gridState,
            onModelClick = onModelClick,
            onHideModel = onHideModel,
            topPadding = topPadding,
            bottomPadding = bottomPadding,
            gridColumns = gridColumns,
            ownedHashes = ownedHashes,
            favoriteIds = favoriteIds,
            onToggleFavorite = onToggleFavorite,
            onCompareModel = onCompareModel,
            isComparing = isComparing,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun SearchContentCrossfade(
    stateKey: String,
    refreshError: Throwable?,
    lazyPagingItems: LazyPagingItems<Model>,
    recommendations: List<RecommendationSection>,
    gridState: LazyGridState,
    onModelClick: (Long, String?, String) -> Unit,
    onHideModel: (Long, String) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    gridColumns: Int,
    ownedHashes: Set<String>,
    favoriteIds: Set<Long>,
    onToggleFavorite: (Model) -> Unit,
    onCompareModel: (Long, String) -> Unit,
    isComparing: Boolean,
) {
    Crossfade(
        targetState = stateKey,
        animationSpec = tween(durationMillis = Duration.normal, easing = Easing.standard),
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
                    recommendations = recommendations,
                    gridState = gridState,
                    onModelClick = onModelClick,
                    onHideModel = onHideModel,
                    topPadding = topPadding,
                    bottomPadding = bottomPadding,
                    gridColumns = gridColumns,
                    ownedHashes = ownedHashes,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = onToggleFavorite,
                    onCompareModel = onCompareModel,
                    isComparing = isComparing,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
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
    ownedHashes: Set<String> = emptySet(),
    favoriteIds: Set<Long> = emptySet(),
    onToggleFavorite: (Model) -> Unit = {},
    onCompareModel: (Long, String) -> Unit = { _, _ -> },
    isComparing: Boolean = false,
) {
    val isAppendLoading = lazyPagingItems.loadState.append is LoadState.Loading
    val reducedMotion = isReducedMotionEnabled()

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
        recommendationItems(recommendations, onModelClick)
        modelPagingItems(
            lazyPagingItems, ownedHashes, favoriteIds,
            isComparing, reducedMotion, onModelClick, onHideModel,
            onToggleFavorite, onCompareModel,
        )
        appendLoadingItem(isAppendLoading)
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.recommendationItems(
    recommendations: List<RecommendationSection>,
    onModelClick: (Long, String?, String) -> Unit,
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
}

@Suppress("LongParameterList")
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.modelPagingItems(
    lazyPagingItems: LazyPagingItems<Model>,
    ownedHashes: Set<String>,
    favoriteIds: Set<Long>,
    isComparing: Boolean,
    reducedMotion: Boolean,
    onModelClick: (Long, String?, String) -> Unit,
    onHideModel: (Long, String) -> Unit,
    onToggleFavorite: (Model) -> Unit,
    onCompareModel: (Long, String) -> Unit,
) {
    items(
        count = lazyPagingItems.itemCount,
        key = lazyPagingItems.itemKey { it.id },
        contentType = { "model" },
    ) { index ->
        val model = lazyPagingItems[index] ?: return@items
        val thumbnailUrl = model.modelVersions
            .firstOrNull()?.images?.firstOrNull()?.thumbnailUrl()
        val isOwned = ownedHashes.isNotEmpty() && model.isOwnedBy(ownedHashes)

        val staggerAnimatable = remember { Animatable(0f) }
        LaunchStaggerAnimation(index = index, animatable = staggerAnimatable, reducedMotion = reducedMotion)
        ModelGridItem(
            model = model,
            isFavorite = model.id in favoriteIds,
            thumbnailUrl = thumbnailUrl,
            isOwned = isOwned,
            isComparing = isComparing,
            onModelClick = onModelClick,
            onHideModel = onHideModel,
            onToggleFavorite = onToggleFavorite,
            onCompareModel = onCompareModel,
            modifier = Modifier.animateItem(),
        )
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.appendLoadingItem(
    isAppendLoading: Boolean,
) {
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

@Suppress("LongParameterList")
@Composable
private fun ModelGridItem(
    model: Model,
    isFavorite: Boolean,
    thumbnailUrl: String?,
    isOwned: Boolean,
    isComparing: Boolean,
    onModelClick: (Long, String?, String) -> Unit,
    onHideModel: (Long, String) -> Unit,
    onToggleFavorite: (Model) -> Unit,
    onCompareModel: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        SwipeableModelCard(
            model = model,
            isFavorite = isFavorite,
            onFavoriteToggle = { onToggleFavorite(model) },
            onHide = { onHideModel(model.id, model.name) },
            onClick = { onModelClick(model.id, thumbnailUrl, "") },
            onLongPress = { showMenu = true },
            isOwned = isOwned,
        )
        ModelContextMenu(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            showCompare = !isComparing,
            onCompare = { onCompareModel(model.id, model.name) },
            onHide = { onHideModel(model.id, model.name) },
        )
    }
}

@Composable
private fun ModelContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    showCompare: Boolean,
    onCompare: () -> Unit,
    onHide: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (showCompare) {
            DropdownMenuItem(
                text = { Text("Compare") },
                onClick = {
                    onCompare()
                    onDismiss()
                },
                leadingIcon = {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.cd_compare))
                },
            )
        }
        DropdownMenuItem(
            text = { Text("Hide model") },
            onClick = {
                onHide()
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.VisibilityOff, contentDescription = stringResource(R.string.cd_hide_model))
            },
        )
    }
}

@Composable
internal fun ComparisonBottomBar(
    compareModelName: String?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = compareModelName != null,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = stringResource(R.string.cd_compare_models),
                    modifier = Modifier.padding(end = Spacing.sm),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Comparing: ${compareModelName ?: ""}",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Tap another model to compare",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun RecommendationRow(
    section: RecommendationSection,
    sharedElementSuffix: String,
    onModelClick: (Long, String?, String) -> Unit,
) {
    val cardWidth = if (isExpandedWidth()) 200.dp else 160.dp
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
                        .width(cardWidth),
                    sharedElementSuffix = sharedElementSuffix,
                )
            }
        }
    }
}

private fun Model.isOwnedBy(ownedHashes: Set<String>): Boolean =
    modelVersions.any { version ->
        version.files.any { file ->
            file.hashes["SHA256"]?.lowercase() in ownedHashes
        }
    }
