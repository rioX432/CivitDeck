package com.riox432.civitdeck.ui.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.crossfade
import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.search.ComparisonBottomBar
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.IconSize
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer
import com.riox432.civitdeck.util.FormatUtils

@Composable
fun FavoritesScreen(
    favorites: List<FavoriteModelSummary>,
    onModelClick: (Long) -> Unit,
    gridColumns: Int = 2,
    scrollToTopTrigger: Int = 0,
    onCompareModel: (Long, String) -> Unit = { _, _ -> },
    compareModelName: String? = null,
    onCancelCompare: () -> Unit = {},
) {
    val gridState = rememberLazyGridState()

    var lastHandledTrigger by rememberSaveable { mutableIntStateOf(scrollToTopTrigger) }
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger != lastHandledTrigger) {
            lastHandledTrigger = scrollToTopTrigger
            gridState.animateScrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (favorites.isEmpty()) {
            EmptyFavorites()
        } else {
            FavoritesGrid(
                favorites = favorites,
                onModelClick = onModelClick,
                onCompareModel = onCompareModel,
                gridColumns = gridColumns,
                gridState = gridState,
            )
        }

        ComparisonBottomBar(
            compareModelName = compareModelName,
            onCancel = onCancelCompare,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun EmptyFavorites() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No favorites yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Models you favorite will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FavoritesGrid(
    favorites: List<FavoriteModelSummary>,
    onModelClick: (Long) -> Unit,
    onCompareModel: (Long, String) -> Unit,
    topPadding: Dp = 0.dp,
    gridColumns: Int = 2,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState = rememberLazyGridState(),
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        state = gridState,
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.sm + topPadding,
            bottom = Spacing.lg,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = favorites, key = { it.id }) { model ->
            FavoriteCardWithContextMenu(
                model = model,
                onClick = { onModelClick(model.id) },
                onCompare = { onCompareModel(model.id, model.name) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteCardWithContextMenu(
    model: FavoriteModelSummary,
    onClick: () -> Unit,
    onCompare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = { showMenu = true },
        ),
    ) {
        FavoriteCard(model = model)
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text("Compare") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null)
                },
                onClick = {
                    showMenu = false
                    onCompare()
                },
            )
        }
    }
}

@Composable
private fun FavoriteCard(
    model: FavoriteModelSummary,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CornerRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column {
            if (model.thumbnailUrl != null) {
                SubcomposeAsyncImage(
                    model = coil3.request.ImageRequest.Builder(
                        androidx.compose.ui.platform.LocalContext.current,
                    )
                        .data(model.thumbnailUrl)
                        .crossfade(Duration.normal)
                        .build(),
                    contentDescription = model.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .shimmer(),
                        )
                    },
                    error = {
                        ImageErrorPlaceholder(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                        )
                    },
                )
            }

            FavoriteCardInfo(model = model)
        }
    }
}

@Composable
private fun FavoriteCardInfo(model: FavoriteModelSummary) {
    Column(
        modifier = Modifier.padding(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = model.type.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(CornerRadius.chip),
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FavoriteStatItem(
                label = FormatUtils.formatCount(model.downloadCount),
                icon = Icons.Outlined.Download,
            )
            FavoriteStatItem(
                label = FormatUtils.formatCount(model.favoriteCount),
                icon = Icons.Outlined.FavoriteBorder,
            )
            FavoriteStatItem(
                label = FormatUtils.formatRating(model.rating),
                icon = Icons.Outlined.Star,
            )
        }
    }
}

@Composable
private fun FavoriteStatItem(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(IconSize.statIcon),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
