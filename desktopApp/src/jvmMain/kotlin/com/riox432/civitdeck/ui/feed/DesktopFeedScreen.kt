package com.riox432.civitdeck.ui.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.presentation.feed.FeedViewModel
import com.riox432.civitdeck.presentation.settings.DisplaySettingsViewModel
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer
import com.riox432.civitdeck.ui.theme.Elevation
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DesktopFeedScreen(
    viewModel: FeedViewModel,
    onModelClick: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        FeedTopBar(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
        )

        when {
            uiState.isLoading && uiState.feedItems.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null && uiState.feedItems.isEmpty() -> {
                FeedErrorView(
                    error = uiState.error ?: "Unknown error",
                    onRetry = viewModel::refresh,
                )
            }
            uiState.feedItems.isEmpty() -> FeedEmptyView()
            else -> FeedGrid(
                items = uiState.feedItems,
                onModelClick = onModelClick,
                onCreatorClick = onCreatorClick,
            )
        }
    }
}

@Composable
private fun FeedTopBar(isRefreshing: Boolean, onRefresh: () -> Unit) {
    Surface(tonalElevation = Elevation.xs) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Feed",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = Spacing.sm),
            )
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(REFRESH_INDICATOR_SIZE))
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }
    }
}

@Composable
private fun FeedEmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.RssFeed,
                contentDescription = "Empty feed",
                modifier = Modifier.size(EMPTY_ICON_SIZE),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "No feed items yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = "Follow creators to see their latest models here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeedErrorView(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            androidx.compose.material3.TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun FeedGrid(
    items: List<FeedItem>,
    onModelClick: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
) {
    val displayViewModel: DisplaySettingsViewModel = koinViewModel()
    val displayState by displayViewModel.uiState.collectAsState()
    val columns = displayState.gridColumns

    LazyVerticalGrid(
        columns = if (columns > 0) GridCells.Fixed(columns) else GridCells.Adaptive(minSize = CARD_MIN_WIDTH),
        contentPadding = PaddingValues(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = items, key = { it.modelId }) { item ->
            FeedGridCard(
                item = item,
                onModelClick = { onModelClick(item.modelId) },
                onCreatorClick = { onCreatorClick(item.creatorUsername) },
            )
        }
    }
}

@Composable
private fun FeedGridCard(
    item: FeedItem,
    onModelClick: () -> Unit,
    onCreatorClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onModelClick),
        shape = RoundedCornerShape(CornerRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.xs),
    ) {
        Column {
            if (item.thumbnailUrl != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(PlatformContext.INSTANCE)
                        .data(item.thumbnailUrl)
                        .size(Size(FEED_THUMBNAIL_SIZE, FEED_THUMBNAIL_SIZE))
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(THUMBNAIL_RATIO)
                        .clip(RoundedCornerShape(topStart = CornerRadius.card, topEnd = CornerRadius.card)),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(THUMBNAIL_RATIO)
                                .shimmer(),
                        )
                    },
                )
            }
            Column(modifier = Modifier.padding(Spacing.sm)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                FeedItemMeta(item = item, onCreatorClick = onCreatorClick)
            }
        }
    }
}

@Composable
private fun FeedItemMeta(
    item: FeedItem,
    onCreatorClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = item.creatorUsername,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onCreatorClick),
        )
        Spacer(modifier = Modifier.width(Spacing.xs))
        Text(
            text = item.type.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.isUnread) {
            Spacer(modifier = Modifier.width(Spacing.xs))
            val unreadColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.size(UNREAD_DOT_SIZE)) {
                drawCircle(color = unreadColor)
            }
        }
    }
}

private val EMPTY_ICON_SIZE = 48.dp
private val REFRESH_INDICATOR_SIZE = 24.dp
private val UNREAD_DOT_SIZE = 6.dp
private val CARD_MIN_WIDTH = 200.dp
private const val THUMBNAIL_RATIO = 3f / 4f
private const val FEED_THUMBNAIL_SIZE = 400
