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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    models: List<Model>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    error: String?,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    callbacks: ModelGridCallbacks,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    gridColumns: Int = 2,
    ownedHashes: Set<String> = emptySet(),
    favoriteIds: Set<Long> = emptySet(),
    isComparing: Boolean = false,
) {
    val stateKey = when {
        isLoading && models.isEmpty() -> "loading"
        error != null && models.isEmpty() -> "error"
        else -> "content"
    }

    SearchContentPullToRefresh(
        isRefreshing = isLoading && models.isNotEmpty(),
        onRefresh = onRefresh,
        topPadding = topPadding,
        stateKey = stateKey,
        error = error,
        recommendations = recommendations,
        gridState = gridState,
        models = models,
        isLoadingMore = isLoadingMore,
        onLoadMore = onLoadMore,
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
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    topPadding: androidx.compose.ui.unit.Dp,
    stateKey: String,
    error: String?,
    recommendations: List<RecommendationSection>,
    gridState: LazyGridState,
    models: List<Model>,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    callbacks: ModelGridCallbacks,
    bottomPadding: androidx.compose.ui.unit.Dp,
    gridColumns: Int,
    ownedHashes: Set<String>,
    favoriteIds: Set<Long>,
    isComparing: Boolean,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = topPadding),
            )
        },
    ) {
        SearchContentCrossfade(
            stateKey = stateKey,
            error = error,
            models = models,
            isLoadingMore = isLoadingMore,
            onLoadMore = onLoadMore,
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
    error: String?,
    models: List<Model>,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
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
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            else -> {
                ModelGrid(
                    models = models,
                    isLoadingMore = isLoadingMore,
                    onLoadMore = onLoadMore,
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
    models: List<Model>,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
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
    val reducedMotion = isReducedMotionEnabled()

    // Trigger load-more when approaching end of list
    LaunchedEffect(gridState, models.size) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to layoutInfo.totalItemsCount
        }.collect { (lastVisible, totalItems) ->
            if (totalItems > 0 && lastVisible >= totalItems - LOAD_MORE_THRESHOLD) {
                onLoadMore()
            }
        }
    }

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
        modelItems(
            models,
            ownedHashes,
            favoriteIds,
            isComparing,
            reducedMotion,
            callbacks,
        )
        appendLoadingItem(isLoadingMore)
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

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.modelItems(
    models: List<Model>,
    ownedHashes: Set<String>,
    favoriteIds: Set<Long>,
    isComparing: Boolean,
    reducedMotion: Boolean,
    callbacks: ModelGridCallbacks,
) {
    items(
        count = models.size,
        key = { models[it].id },
        contentType = { "model" },
    ) { index ->
        val model = models[index]
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

private const val LOAD_MORE_THRESHOLD = 10
