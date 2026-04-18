package com.riox432.civitdeck.ui.notificationcenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ModelUpdateNotification
import com.riox432.civitdeck.domain.model.UpdateSource
import com.riox432.civitdeck.feature.gallery.presentation.NotificationCenterViewModel
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.theme.Spacing
import java.util.concurrent.TimeUnit

/** Size of the unread indicator dot. */
// TODO: Unify with shared design token
private val UnreadDotSize = Spacing.sm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    viewModel: NotificationCenterViewModel,
    onBack: () -> Unit,
    onNavigateToModel: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                actions = {
                    if (state.notifications.any { !it.isRead }) {
                        IconButton(onClick = { viewModel.markAllRead() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = stringResource(R.string.cd_mark_all_read))
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.notifications.isEmpty() -> {
                EmptyStateMessage(
                    icon = Icons.Default.NotificationsNone,
                    title = "No update notifications yet",
                    subtitle = "Model updates will appear here",
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }
            else -> {
                NotificationList(
                    notifications = state.notifications,
                    onItemClick = { notification ->
                        viewModel.markRead(notification.id)
                        onNavigateToModel(notification.modelId)
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun NotificationList(
    notifications: List<ModelUpdateNotification>,
    onItemClick: (ModelUpdateNotification) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(notifications, key = { it.id }) { notification ->
            NotificationRow(notification, onClick = { onItemClick(notification) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun NotificationRow(
    notification: ModelUpdateNotification,
    onClick: () -> Unit,
) {
    val bgColor = if (!notification.isRead) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!notification.isRead) {
            Box(
                Modifier
                    .size(UnreadDotSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        } else {
            Box(Modifier.size(UnreadDotSize))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.modelName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (!notification.isRead) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                text = "New version: ${notification.newVersionName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = sourceLabel(notification.source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = relativeTimeLabel(notification.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun sourceLabel(source: UpdateSource): String = when (source) {
    UpdateSource.FAVORITE -> "Favorite"
    UpdateSource.FOLLOWED -> "Followed Creator"
}

private fun relativeTimeLabel(epochMs: Long): String {
    val now = System.currentTimeMillis()
    val diffMs = now - epochMs
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 2 -> "Yesterday"
        days < 30 -> "${days}d ago"
        else -> "${days / 30}mo ago"
    }
}
