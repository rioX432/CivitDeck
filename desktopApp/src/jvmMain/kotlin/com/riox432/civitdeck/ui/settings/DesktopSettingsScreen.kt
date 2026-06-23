package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.StorageSettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.update.DesktopUpdateViewModel

@Composable
// Compose UI: state/callback params are an intrinsic UI contract; a param object only hides them.
@Suppress("LongParameterList")
fun DesktopSettingsScreen(
    authSettingsViewModel: AuthSettingsViewModel,
    displaySettingsViewModel: DisplaySettingsViewModel,
    contentFilterSettingsViewModel: ContentFilterSettingsViewModel,
    appBehaviorSettingsViewModel: AppBehaviorSettingsViewModel,
    storageSettingsViewModel: StorageSettingsViewModel,
    updateViewModel: DesktopUpdateViewModel,
    onNavigateToDatasets: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToPlugins: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToNotificationCenter: () -> Unit = {},
    onNavigateToBrowsingHistory: () -> Unit = {},
    onNavigateToDownloadQueue: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        AuthSettingsSection(authSettingsViewModel)
        ToolsSection(
            onNavigateToDatasets = onNavigateToDatasets,
            onNavigateToBackup = onNavigateToBackup,
            onNavigateToPlugins = onNavigateToPlugins,
            onNavigateToAnalytics = onNavigateToAnalytics,
            onNavigateToNotificationCenter = onNavigateToNotificationCenter,
            onNavigateToBrowsingHistory = onNavigateToBrowsingHistory,
            onNavigateToDownloadQueue = onNavigateToDownloadQueue,
        )
        DisplaySettingsSection(displaySettingsViewModel)
        ContentFilterSection(contentFilterSettingsViewModel)
        AppBehaviorSection(appBehaviorSettingsViewModel)
        UpdateSection(updateViewModel)
        StorageSection(storageSettingsViewModel)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolsSection(
    onNavigateToDatasets: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToNotificationCenter: () -> Unit,
    onNavigateToBrowsingHistory: () -> Unit,
    onNavigateToDownloadQueue: () -> Unit,
) {
    SettingsCard(title = "Tools") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedButton(onClick = onNavigateToDatasets) { Text("Datasets") }
            OutlinedButton(onClick = onNavigateToBackup) { Text("Backup & Restore") }
            OutlinedButton(onClick = onNavigateToPlugins) { Text("Plugins") }
            OutlinedButton(onClick = onNavigateToAnalytics) { Text("Analytics") }
            OutlinedButton(onClick = onNavigateToNotificationCenter) { Text("Notifications") }
            OutlinedButton(onClick = onNavigateToBrowsingHistory) { Text("History") }
            OutlinedButton(onClick = onNavigateToDownloadQueue) { Text("Downloads") }
        }
    }
}
