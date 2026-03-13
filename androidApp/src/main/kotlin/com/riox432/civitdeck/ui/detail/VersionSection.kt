package com.riox432.civitdeck.ui.detail

import android.text.Html
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.riox432.civitdeck.domain.model.HapticFeedbackType
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.model.ModelFile
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.ui.components.FilterChipRow
import com.riox432.civitdeck.ui.components.SectionHeader
import com.riox432.civitdeck.ui.components.rememberHapticFeedback
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun VersionSelector(
    versions: List<ModelVersion>,
    selectedIndex: Int,
    onVersionSelected: (Int) -> Unit,
) {
    val haptic = rememberHapticFeedback()

    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
        SectionHeader(
            title = "Versions",
            modifier = Modifier.padding(horizontal = Spacing.lg),
            showDivider = true,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        FilterChipRow(
            options = versions,
            selected = versions[selectedIndex],
            onSelect = { version ->
                haptic(HapticFeedbackType.Selection)
                onVersionSelected(versions.indexOf(version))
            },
            label = { it.name },
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )
    }
}

@Suppress("LongParameterList")
@Composable
internal fun VersionDetail(
    version: ModelVersion,
    powerUserMode: Boolean = false,
    downloads: Map<Long, ModelDownload> = emptyMap(),
    onDownloadFile: (ModelFile) -> Unit = {},
    onCancelDownload: (Long) -> Unit = {},
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.sm)) {
        if (version.baseModel != null) {
            DetailRow(label = "Base Model", value = version.baseModel!!)
        }

        if (version.trainedWords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            SectionHeader(title = "Trained Words", showDivider = false)
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = version.trainedWords.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (version.files.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.md))
            SectionHeader(title = "Files", showDivider = false)
            Spacer(modifier = Modifier.height(Spacing.xs))
            version.files.forEach { file ->
                FileDownloadRow(
                    file = file,
                    downloadState = downloads[file.id],
                    onDownload = onDownloadFile,
                    onCancel = onCancelDownload,
                )
                if (powerUserMode) {
                    AdvancedFileInfo(file = file)
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
        }

        if (powerUserMode) {
            AdvancedVersionInfo(version = version)
        }
    }
}

@Composable
private fun AdvancedFileInfo(file: ModelFile) {
    Column(modifier = Modifier.padding(start = Spacing.sm)) {
        file.hashes.forEach { (algorithm, hash) ->
            DetailRow(label = algorithm, value = hash)
        }
        file.pickleScanResult?.let { DetailRow(label = "Pickle Scan", value = it) }
        file.virusScanResult?.let { DetailRow(label = "Virus Scan", value = it) }
        file.scannedAt?.let { DetailRow(label = "Scanned At", value = it) }
    }
}

@Composable
private fun AdvancedVersionInfo(version: ModelVersion) {
    var expanded by remember { mutableStateOf(false) }
    Spacer(modifier = Modifier.height(Spacing.md))
    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Advanced Info",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = if (expanded) "Hide" else "Show",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (expanded) {
        Column(modifier = Modifier.animateContentSize()) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            version.createdAt.takeIf { it.isNotBlank() }?.let {
                DetailRow(label = "Created", value = it)
            }
            version.stats?.let { stats ->
                DetailRow(label = "Downloads", value = stats.downloadCount.toString())
                DetailRow(label = "Rating", value = "${stats.rating} (${stats.ratingCount})")
            }
            version.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Version Notes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = Html.fromHtml(desc, Html.FROM_HTML_MODE_COMPACT).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
