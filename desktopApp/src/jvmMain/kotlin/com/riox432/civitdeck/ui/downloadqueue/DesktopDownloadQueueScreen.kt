@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.downloadqueue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.util.FormatUtils
import com.riox432.civitdeck.ui.theme.Elevation
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun DesktopDownloadQueueScreen(
    viewModel: DesktopDownloadQueueViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    Surface(modifier = modifier.fillMaxSize()) {
        Column {
            DownloadQueueToolbar(onBack = onBack)
            DownloadQueueBody(
                state = state,
                onPause = viewModel::pauseDownload,
                onResume = viewModel::resumeDownload,
                onCancel = viewModel::cancelDownload,
                onRetry = viewModel::retryDownload,
                onDelete = viewModel::deleteDownload,
                onClearCompleted = viewModel::clearCompleted,
            )
        }
    }
}

@Composable
private fun DownloadQueueToolbar(onBack: () -> Unit) {
    Surface(tonalElevation = Elevation.xs) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Download Queue",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun DownloadQueueBody(
    state: DesktopDownloadQueueUiState,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onClearCompleted: () -> Unit,
) {
    val isEmpty = state.activeDownloads.isEmpty() &&
        state.completedDownloads.isEmpty() &&
        state.failedDownloads.isEmpty()

    if (isEmpty && !state.isLoading) {
        EmptyDownloadQueue()
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (state.activeDownloads.isNotEmpty()) {
            item { SectionHeader("Active") }
            items(state.activeDownloads, key = { it.id }) { download ->
                ActiveDownloadItem(download, onPause, onResume, onCancel)
            }
        }
        if (state.failedDownloads.isNotEmpty()) {
            item { SectionHeader("Failed") }
            items(state.failedDownloads, key = { it.id }) { download ->
                FailedDownloadItem(download, onRetry, onDelete)
            }
        }
        if (state.completedDownloads.isNotEmpty()) {
            item { CompletedSectionHeader(onClearCompleted) }
            items(state.completedDownloads, key = { it.id }) { download ->
                CompletedDownloadItem(download, onDelete)
            }
        }
        if (state.totalStorageBytes > 0) {
            item {
                Text(
                    text = "Total storage: ${FormatUtils.formatFileSize(state.totalStorageBytes / KB_DIVISOR)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.md),
                )
            }
        }
    }
}

@Composable
private fun EmptyDownloadQueue() {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(EMPTY_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "No downloads",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.md),
        )
        Text(
            text = "Downloads you start will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
    )
}

@Composable
private fun CompletedSectionHeader(onClearCompleted: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Completed",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        TextButton(onClick = onClearCompleted) {
            Text("Clear All")
        }
    }
}

@Composable
private fun ActiveDownloadItem(
    download: ModelDownload,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = download.modelName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = download.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ActiveDownloadActions(download, onPause, onResume, onCancel)
        }
        DownloadProgressBar(download)
    }
}

@Composable
private fun ActiveDownloadActions(
    download: ModelDownload,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (download.status) {
            DownloadStatus.Downloading -> {
                IconButton(onClick = { onPause(download.id) }) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            DownloadStatus.Paused -> {
                IconButton(onClick = { onResume(download.id) }) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            DownloadStatus.Pending -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(INDICATOR_SIZE),
                    strokeWidth = 2.dp,
                )
            }
            else -> {}
        }
        IconButton(onClick = { onCancel(download.id) }) {
            Icon(
                Icons.Default.Cancel,
                contentDescription = "Cancel",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun DownloadProgressBar(download: ModelDownload) {
    val progress = if (download.fileSizeBytes > 0) {
        download.downloadedBytes.toFloat() / download.fileSizeBytes
    } else {
        0f
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.padding(top = Spacing.xs),
    ) {
        if (download.status == DownloadStatus.Paused) {
            Text(
                text = "Paused",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${(progress * PERCENT_MAX).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FailedDownloadItem(
    download: ModelDownload,
    onRetry: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.modelName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = download.errorMessage ?: download.status.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = { onRetry(download.id) }) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Retry",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = { onDelete(download.id) }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun CompletedDownloadItem(
    download: ModelDownload,
    onDelete: (Long) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.modelName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = FormatUtils.formatFileSize(download.fileSizeBytes / KB_DIVISOR),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HashBadge(download)
            }
        }
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = "Downloaded",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(Spacing.sm),
        )
        IconButton(onClick = { onDelete(download.id) }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun HashBadge(download: ModelDownload) {
    when (download.hashVerified) {
        true -> Text(
            text = "Verified",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        false -> Text(
            text = "Hash mismatch",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
        null -> {}
    }
}

private val INDICATOR_SIZE = 24.dp
private val EMPTY_ICON_SIZE = 64.dp
private const val PERCENT_MAX = 100
private const val KB_DIVISOR = 1024.0
