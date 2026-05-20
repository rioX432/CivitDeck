package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.settings.presentation.StorageSettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@Suppress("LongMethod")
@Composable
internal fun StorageSection(viewModel: StorageSettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearSearchDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    SettingsCard(title = "Storage & Cache") {
        StorageStatus(isOnline = state.isOnline)
        Spacer(modifier = Modifier.height(Spacing.sm))
        SwitchSetting(
            label = "Offline Cache",
            checked = state.offlineCacheEnabled,
            onCheckedChange = viewModel::onOfflineCacheEnabledChanged,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        SliderSetting(
            label = "Cache Size Limit",
            value = state.cacheSizeLimitMb.toFloat(),
            valueRange = 50f..1000f,
            steps = 18,
            valueLabel = "${state.cacheSizeLimitMb} MB",
            onValueChange = { viewModel.onCacheSizeLimitChanged(it.toInt()) },
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = "Cache: ${state.cacheInfo.sizeBytes / 1024 / 1024} MB " +
                "(${state.cacheInfo.entryCount} entries)",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedButton(onClick = { showClearCacheDialog = true }) { Text("Clear Cache") }
            OutlinedButton(onClick = { showClearSearchDialog = true }) { Text("Clear Search") }
            OutlinedButton(onClick = { showClearHistoryDialog = true }) { Text("Clear History") }
        }
    }

    ClearCacheDialog(
        visible = showClearCacheDialog,
        onDismiss = { showClearCacheDialog = false },
        onConfirm = {
            viewModel.onClearCache()
            showClearCacheDialog = false
        },
    )
    ClearSearchDialog(
        visible = showClearSearchDialog,
        onDismiss = { showClearSearchDialog = false },
        onConfirm = {
            viewModel.onClearSearchHistory()
            showClearSearchDialog = false
        },
    )
    ClearHistoryDialog(
        visible = showClearHistoryDialog,
        onDismiss = { showClearHistoryDialog = false },
        onConfirm = {
            viewModel.onClearBrowsingHistory()
            showClearHistoryDialog = false
        },
    )
}

@Composable
private fun StorageStatus(isOnline: Boolean) {
    Text(
        text = if (isOnline) "Status: Online" else "Status: Offline",
        style = MaterialTheme.typography.bodySmall,
        color = if (isOnline) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
    )
}

@Composable
private fun ClearCacheDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (visible) {
        ConfirmClearDialog(
            title = "Clear Cache",
            message = "Are you sure you want to clear all cached data? This cannot be undone.",
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        )
    }
}

@Composable
private fun ClearSearchDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (visible) {
        ConfirmClearDialog(
            title = "Clear Search History",
            message = "Are you sure you want to clear your search history? This cannot be undone.",
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        )
    }
}

@Composable
private fun ClearHistoryDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (visible) {
        ConfirmClearDialog(
            title = "Clear Browsing History",
            message = "Are you sure you want to clear your browsing history? This cannot be undone.",
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        )
    }
}

@Composable
private fun ConfirmClearDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Clear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
