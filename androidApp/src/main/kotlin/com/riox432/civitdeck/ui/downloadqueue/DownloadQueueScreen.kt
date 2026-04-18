package com.riox432.civitdeck.ui.downloadqueue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.util.FormatUtils
import com.riox432.civitdeck.feature.gallery.presentation.DownloadQueueUiState
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadQueueScreen(
    uiState: DownloadQueueUiState,
    onBack: () -> Unit,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onClearCompleted: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.download_queue_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        DownloadQueueContent(
            uiState = uiState,
            contentPadding = padding,
            onPause = onPause,
            onResume = onResume,
            onCancel = onCancel,
            onRetry = onRetry,
            onDelete = onDelete,
            onClearCompleted = onClearCompleted,
        )
    }
}

@Composable
private fun DownloadQueueContent(
    uiState: DownloadQueueUiState,
    contentPadding: PaddingValues,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onClearCompleted: () -> Unit,
) {
    val isEmpty = uiState.activeDownloads.isEmpty() &&
        uiState.completedDownloads.isEmpty() &&
        uiState.failedDownloads.isEmpty()

    if (isEmpty && !uiState.isLoading) {
        EmptyStateMessage(
            icon = Icons.Default.CloudDownload,
            title = stringResource(R.string.download_queue_empty),
            subtitle = stringResource(R.string.download_queue_empty_desc),
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
        return
    }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (uiState.activeDownloads.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.download_queue_active)) }
            items(uiState.activeDownloads, key = { it.id }) { download ->
                ActiveDownloadItem(download, onPause, onResume, onCancel)
            }
        }

        if (uiState.failedDownloads.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.download_queue_failed)) }
            items(uiState.failedDownloads, key = { it.id }) { download ->
                FailedDownloadItem(download, onRetry, onDelete)
            }
        }

        if (uiState.completedDownloads.isNotEmpty()) {
            item { CompletedSectionHeader(onClearCompleted) }
            items(uiState.completedDownloads, key = { it.id }) { download ->
                CompletedDownloadItem(download, onDelete)
            }
        }

        if (uiState.totalStorageBytes > 0) {
            item {
                Text(
                    text = stringResource(
                        R.string.download_queue_storage_used,
                        FormatUtils.formatFileSize(uiState.totalStorageBytes / KB_DIVISOR),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.md),
                )
            }
        }
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
            text = stringResource(R.string.download_queue_completed),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        TextButton(onClick = onClearCompleted) {
            Text(stringResource(R.string.download_queue_clear_completed))
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
    var prevBytes by remember { mutableLongStateOf(download.downloadedBytes) }
    var prevTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var speedText by remember { mutableStateOf("") }

    LaunchedEffect(download.downloadedBytes) {
        val now = System.currentTimeMillis()
        val elapsed = now - prevTime
        if (elapsed > SPEED_SAMPLE_INTERVAL_MS && download.status == DownloadStatus.Downloading) {
            val bytesDelta = download.downloadedBytes - prevBytes
            val speedBps = bytesDelta.toDouble() / (elapsed / MS_PER_SECOND_DOUBLE)
            val speedMbps = speedBps / BYTES_PER_MB
            val remaining = download.fileSizeBytes - download.downloadedBytes
            val etaSeconds = if (speedBps > 0) (remaining / speedBps).toLong() else 0L
            val etaLabel = formatEta(etaSeconds)
            speedText = "%.1f MB/s".format(speedMbps) + if (etaLabel.isNotEmpty()) " \u2022 $etaLabel" else ""
            prevBytes = download.downloadedBytes
            prevTime = now
        }
    }

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
            DownloadActions(download, onPause, onResume, onCancel)
        }
        DownloadProgress(download)
        if (speedText.isNotEmpty() && download.status == DownloadStatus.Downloading) {
            Text(
                text = speedText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

@Composable
private fun DownloadActions(
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
                        contentDescription = stringResource(R.string.cd_pause_download),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            DownloadStatus.Paused -> {
                IconButton(onClick = { onResume(download.id) }) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.cd_resume_download),
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
                contentDescription = stringResource(R.string.cd_cancel_download),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun DownloadProgress(download: ModelDownload) {
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
                text = stringResource(R.string.download_queue_paused),
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
                contentDescription = stringResource(R.string.cd_retry_download),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = { onDelete(download.id) }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
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
            contentDescription = stringResource(R.string.cd_downloaded),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(Spacing.sm),
        )
        IconButton(onClick = { onDelete(download.id) }) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun HashBadge(download: ModelDownload) {
    when (download.hashVerified) {
        true -> Text(
            text = stringResource(R.string.download_queue_hash_verified),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        false -> Text(
            text = stringResource(R.string.download_queue_hash_mismatch),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
        null -> {}
    }
}

private fun formatEta(seconds: Long): String = when {
    seconds <= 0 -> ""
    seconds < SECONDS_PER_MINUTE -> "~${seconds}s remaining"
    seconds < SECONDS_PER_HOUR -> "~${seconds / SECONDS_PER_MINUTE}m remaining"
    else -> "~${seconds / SECONDS_PER_HOUR}h ${(seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE}m remaining"
}

private val INDICATOR_SIZE = 24.dp
private const val PERCENT_MAX = 100
private const val KB_DIVISOR = 1024.0
private const val SPEED_SAMPLE_INTERVAL_MS = 500L
private const val MS_PER_SECOND_DOUBLE = 1000.0
private const val BYTES_PER_MB = 1_048_576.0
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3600L
