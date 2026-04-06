package com.riox432.civitdeck.ui.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.presentation.feed.FeedUiState
import com.riox432.civitdeck.presentation.feed.FeedViewModel
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onBack: (() -> Unit)?,
    onModelClick: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feed") },
                navigationIcon = {
                    if (onBack != null) {
                        androidx.compose.material3.IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_back),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        FeedContent(
            uiState = uiState,
            onRefresh = viewModel::refresh,
            onModelClick = onModelClick,
            onCreatorClick = onCreatorClick,
            contentPadding = padding,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedContent(
    uiState: FeedUiState,
    onRefresh: () -> Unit,
    onModelClick: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    when {
        uiState.isLoading && uiState.feedItems.isEmpty() -> LoadingStateOverlay()
        uiState.error != null && uiState.feedItems.isEmpty() -> {
            ErrorStateView(
                message = uiState.error ?: "Unknown error",
                onRetry = onRefresh,
            )
        }
        uiState.feedItems.isEmpty() && !uiState.isLoading -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = contentPadding.calculateTopPadding()),
            ) {
                EmptyStateMessage(
                    icon = Icons.Default.RssFeed,
                    title = "No feed items yet",
                    subtitle = "Follow creators to see their latest models here.",
                )
            }
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = contentPadding.calculateTopPadding()),
            ) {
                FeedGrid(
                    items = uiState.feedItems,
                    onModelClick = onModelClick,
                    onCreatorClick = onCreatorClick,
                    bottomPadding = contentPadding.calculateBottomPadding(),
                )
            }
        }
    }
}

@Composable
private fun FeedGrid(
    items: List<FeedItem>,
    onModelClick: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = CARD_MIN_WIDTH),
        contentPadding = PaddingValues(
            start = Spacing.sm,
            end = Spacing.sm,
            top = Spacing.sm,
            bottom = Spacing.lg + bottomPadding,
        ),
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
            .clickable(onClick = onModelClick, onClickLabel = "View model details"),
        shape = RoundedCornerShape(CornerRadius.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            if (item.thumbnailUrl != null) {
                CivitAsyncImage(
                    imageUrl = item.thumbnailUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(THUMBNAIL_RATIO)
                        .clip(RoundedCornerShape(topStart = CornerRadius.card, topEnd = CornerRadius.card)),
                    contentScale = ContentScale.Crop,
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
            modifier = Modifier.clickable(onClick = onCreatorClick, onClickLabel = "View creator profile"),
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

private val CARD_MIN_WIDTH = 160.dp
private val UNREAD_DOT_SIZE = 6.dp
private const val THUMBNAIL_RATIO = 3f / 4f
