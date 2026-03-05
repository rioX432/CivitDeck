package com.riox432.civitdeck.ui.feed

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onBack: () -> Unit,
    onModelClick: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feed") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            EmptyStateMessage(
                icon = Icons.Default.RssFeed,
                title = "No feed items yet",
                subtitle = "Follow creators to see their latest models here.",
            )
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = contentPadding.calculateTopPadding()),
            ) {
                FeedList(
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
private fun FeedList(
    items: List<FeedItem>,
    onModelClick: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = Spacing.md,
            end = Spacing.md,
            top = Spacing.sm,
            bottom = Spacing.lg + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(items = items, key = { it.modelId }) { item ->
            FeedItemCard(
                item = item,
                onModelClick = { onModelClick(item.modelId) },
                onCreatorClick = { onCreatorClick(item.creatorUsername) },
            )
        }
    }
}

@Composable
private fun FeedItemCard(
    item: FeedItem,
    onModelClick: () -> Unit,
    onCreatorClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onModelClick, onClickLabel = "View model details"),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            if (item.thumbnailUrl != null) {
                CivitAsyncImage(
                    imageUrl = item.thumbnailUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.padding(Spacing.md)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onCreatorClick, onClickLabel = "View creator profile"),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = item.type.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.isUnread) {
            Spacer(modifier = Modifier.width(Spacing.xs))
            val unreadColor = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .padding(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                ) {
                    // Unread dot
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = unreadColor)
                    }
                }
            }
        }
    }
}
