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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.RecommendationSection
import com.riox432.civitdeck.domain.model.thumbnailUrl
import com.riox432.civitdeck.ui.components.LaunchStaggerAnimation
import com.riox432.civitdeck.ui.components.isReducedMotionEnabled
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.Spacing

/**
 * Groups callback parameters for model grid composables to reduce parameter count.
 */
data class ModelGridCallbacks(
    val onModelClick: (Long, String?, String) -> Unit,
    val onHideModel: (Long, String) -> Unit,
    val onToggleFavorite: (Model) -> Unit,
    val onCompareModel: (Long, String) -> Unit,
)

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModelSearchContent(
    recommendations: List<RecommendationSection>,
    gridState: LazyGridState,
    lazyPagingItems: LazyPagingItems<Model>,
    callbacks: ModelGridCallbacks,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    gridColumns: Int = 2,
    ownedHashes: Set<String> = emptySet(),
    favoriteIds: Set<Long> = emptySet(),
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
        callbacks = callbacks,
        bottomPadding = bottomPadding,
        gridColumns = gridColumns,
        ownedHashes = ownedHashes,
        favoriteIds = favoriteIds,
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
    callbacks: ModelGridCallbacks,
    bottomPadding: androidx.compose.ui.unit.Dp,
    gridColumns: Int,
    ownedHashes: Set<String>,
    favoriteIds: Set<Long>,
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
            callbacks = callbacks,
            topPadding = topPadding,
            bottomPadding = bottomPadding,
            gridColumns = gridColumns,
            ownedHashes = ownedHashes,
            favoriteIds = favoriteIds,
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
    callbacks: ModelGridCallbacks,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
    gridColumns: Int,
    ownedHashes: Set<String>,
    favoriteIds: Set<Long>,
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
                    callbacks = callbacks,
                    topPadding = topPadding,
                    bottomPadding = bottomPadding,
                    gridColumns = gridColumns,
                    ownedHashes = ownedHashes,
                    favoriteIds = favoriteIds,
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
    callbacks: ModelGridCallbacks,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    gridColumns: Int = 2,
    ownedHashes: Set<String> = emptySet(),
    favoriteIds: Set<Long> = emptySet(),
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
        recommendationItems(recommendations, callbacks.onModelClick)
        modelPagingItems(
            lazyPagingItems,
            ownedHashes,
            favoriteIds,
            isComparing,
            reducedMotion,
            callbacks,
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

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.modelPagingItems(
    lazyPagingItems: LazyPagingItems<Model>,
    ownedHashes: Set<String>,
    favoriteIds: Set<Long>,
    isComparing: Boolean,
    reducedMotion: Boolean,
    callbacks: ModelGridCallbacks,
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
            callbacks = callbacks,
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

private fun Model.isOwnedBy(ownedHashes: Set<String>): Boolean =
    modelVersions.any { version ->
        version.files.any { file ->
            file.hashes["SHA256"]?.lowercase() in ownedHashes
        }
    }
