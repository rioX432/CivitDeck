package com.riox432.civitdeck.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.model.ModelFile
import com.riox432.civitdeck.domain.util.FormatUtils
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun FileDownloadRow(
    file: ModelFile,
    downloadState: ModelDownload?,
    onDownload: (ModelFile) -> Unit,
    onCancel: (Long) -> Unit,
    onPause: (Long) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = FormatUtils.formatFileSize(file.sizeKB),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                file.format?.let { format ->
                    Text(
                        text = format,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                file.fp?.let { fp ->
                    Text(
                        text = fp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (downloadState?.status == DownloadStatus.Downloading) {
                    val percent = downloadProgress(downloadState)
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        DownloadAction(
            downloadState = downloadState,
            file = file,
            onDownload = onDownload,
            onCancel = onCancel,
            onPause = onPause,
        )
    }
}

@Composable
private fun DownloadAction(
    downloadState: ModelDownload?,
    file: ModelFile,
    onDownload: (ModelFile) -> Unit,
    onCancel: (Long) -> Unit,
    onPause: (Long) -> Unit,
) {
    when (downloadState?.status) {
        null, DownloadStatus.Cancelled, DownloadStatus.Paused -> {
            IconButton(onClick = { onDownload(file) }) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = stringResource(R.string.cd_download),
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
        DownloadStatus.Downloading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { downloadProgress(downloadState) / PERCENT_MAX_F },
                    modifier = Modifier.size(INDICATOR_SIZE),
                    strokeWidth = 2.dp,
                )
                IconButton(onClick = { onPause(downloadState.id) }) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = stringResource(R.string.cd_pause_download),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { onCancel(downloadState.id) }) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = stringResource(R.string.cd_cancel_download),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        DownloadStatus.Completed -> {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.cd_downloaded),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(Spacing.sm),
            )
        }
        DownloadStatus.Failed -> {
            IconButton(onClick = { onDownload(file) }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.cd_retry_download),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun downloadProgress(download: ModelDownload): Int {
    if (download.fileSizeBytes <= 0) return 0
    return ((download.downloadedBytes * PERCENT_MAX) / download.fileSizeBytes).toInt()
        .coerceIn(0, PERCENT_MAX)
}

private val INDICATOR_SIZE = 24.dp
private const val PERCENT_MAX = 100
private const val PERCENT_MAX_F = 100f
