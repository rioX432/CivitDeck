package com.riox432.civitdeck.ui.dataset

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ExportProgress
import com.riox432.civitdeck.plugin.PluginExportFormat
import com.riox432.civitdeck.ui.theme.Spacing
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDatasetSheet(
    imageCount: Int,
    nonTrainableCount: Int,
    availableFormats: List<PluginExportFormat>,
    selectedFormatId: String?,
    onFormatSelected: (String) -> Unit,
    onExport: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLicenseWarning by remember { mutableStateOf(false) }
    val effectiveFormatId = selectedFormatId ?: availableFormats.firstOrNull()?.id

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        ExportSheetContent(
            imageCount = imageCount,
            nonTrainableCount = nonTrainableCount,
            availableFormats = availableFormats,
            selectedFormatId = effectiveFormatId,
            onFormatSelected = onFormatSelected,
            onExport = {
                val formatId = effectiveFormatId ?: return@ExportSheetContent
                if (nonTrainableCount > 0) showLicenseWarning = true else onExport(formatId)
            },
            onDismiss = onDismiss,
        )
    }

    if (showLicenseWarning) {
        LicenseWarningDialog(
            count = nonTrainableCount,
            onConfirm = {
                showLicenseWarning = false
                effectiveFormatId?.let { onExport(it) }
            },
            onDismiss = { showLicenseWarning = false },
        )
    }
}

@Composable
private fun ExportSheetContent(
    imageCount: Int,
    nonTrainableCount: Int,
    availableFormats: List<PluginExportFormat>,
    selectedFormatId: String?,
    onFormatSelected: (String) -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.dataset_export_title),
            style = MaterialTheme.typography.titleMedium,
        )
        ExportFormatSelector(
            formats = availableFormats,
            selectedFormatId = selectedFormatId,
            onFormatSelected = onFormatSelected,
        )
        Text(
            text = stringResource(R.string.dataset_export_trainable_count, imageCount),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (nonTrainableCount > 0) {
            Text(
                text = stringResource(R.string.dataset_export_excluded_count, nonTrainableCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm, androidx.compose.ui.Alignment.End),
        ) {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            Button(
                onClick = onExport,
                enabled = selectedFormatId != null
            ) { Text(stringResource(R.string.action_export)) }
        }
    }
}

@Composable
private fun ExportFormatSelector(
    formats: List<PluginExportFormat>,
    selectedFormatId: String?,
    onFormatSelected: (String) -> Unit,
) {
    if (formats.size <= 1) {
        val format = formats.firstOrNull()
        val formatName = format?.name ?: stringResource(R.string.dataset_export_format_default)
        Text(
            text = stringResource(R.string.dataset_export_format_single, formatName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = stringResource(R.string.dataset_export_format_label),
            style = MaterialTheme.typography.labelMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            formats.forEach { format ->
                FilterChip(
                    selected = format.id == selectedFormatId,
                    onClick = { onFormatSelected(format.id) },
                    label = { Text(format.name) },
                )
            }
        }
        formats.firstOrNull { it.id == selectedFormatId }?.let { selected ->
            Text(
                text = selected.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ExportProgressOverlay(
    progress: ExportProgress,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    when (progress) {
        is ExportProgress.Preparing -> ExportProgressDialog(title = "Preparing export...")
        is ExportProgress.Downloading -> ExportProgressDialog(
            title = "Downloading images...",
            current = progress.current,
            total = progress.total,
        )
        is ExportProgress.WritingManifest -> ExportProgressDialog(title = "Writing manifest...")
        is ExportProgress.Completed -> ExportCompletedDialog(
            outputPath = progress.outputPath,
            warningCount = progress.warningCount,
            onShare = { shareZipFile(context, progress.outputPath) },
            onDismiss = onDismiss,
        )
        is ExportProgress.Failed -> ExportFailedDialog(
            message = progress.message,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ExportProgressDialog(title: String, current: Int = 0, total: Int = 0) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (total > 0) {
                    LinearProgressIndicator(
                        progress = { current.toFloat() / total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "$current / $total",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
    )
}

@Composable
private fun ExportCompletedDialog(
    outputPath: String,
    warningCount: Int,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                onShare()
                onDismiss()
            }) { Text(stringResource(R.string.action_share)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
        title = { Text(stringResource(R.string.dataset_export_complete)) },
        text = {
            Column {
                Text("File ready: ${File(outputPath).name}")
                if (warningCount > 0) {
                    Text(
                        text = "$warningCount images were excluded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Composable
private fun ExportFailedDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
        title = { Text(stringResource(R.string.dataset_export_failed)) },
        text = { Text(message) },
    )
}

@Composable
private fun LicenseWarningDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.dataset_export_anyway)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        title = { Text(stringResource(R.string.dataset_license_warning)) },
        text = {
            Text(stringResource(R.string.dataset_license_warning_message, count))
        },
    )
}

private fun shareZipFile(context: Context, filePath: String) {
    val file = File(filePath)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share dataset"))
}
