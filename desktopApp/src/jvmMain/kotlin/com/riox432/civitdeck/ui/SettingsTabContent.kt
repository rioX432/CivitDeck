package com.riox432.civitdeck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.presentation.settings.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.presentation.settings.AuthSettingsViewModel
import com.riox432.civitdeck.presentation.settings.ContentFilterSettingsViewModel
import com.riox432.civitdeck.presentation.settings.DisplaySettingsViewModel
import com.riox432.civitdeck.presentation.settings.StorageSettingsViewModel
import com.riox432.civitdeck.ui.DesktopRoute
import com.riox432.civitdeck.ui.analytics.DesktopAnalyticsScreen
import com.riox432.civitdeck.ui.analytics.DesktopAnalyticsViewModel
import com.riox432.civitdeck.ui.backup.DesktopBackupScreen
import com.riox432.civitdeck.ui.backup.DesktopBackupViewModel
import com.riox432.civitdeck.ui.downloadqueue.DesktopDownloadQueueScreen
import com.riox432.civitdeck.ui.downloadqueue.DesktopDownloadQueueViewModel
import com.riox432.civitdeck.ui.history.DesktopBrowsingHistoryScreen
import com.riox432.civitdeck.feature.search.presentation.BrowsingHistoryViewModel
import com.riox432.civitdeck.ui.notificationcenter.DesktopNotificationCenterScreen
import com.riox432.civitdeck.ui.notificationcenter.DesktopNotificationCenterViewModel
import com.riox432.civitdeck.ui.plugin.DesktopPluginDetailScreen
import com.riox432.civitdeck.ui.plugin.DesktopPluginListScreen
import com.riox432.civitdeck.ui.plugin.DesktopPluginViewModel
import com.riox432.civitdeck.ui.settings.DesktopSettingsScreen
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.update.DesktopUpdateViewModel
import com.riox432.civitdeck.util.removeLastOrNull
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsTabContent(
    backstack: SnapshotStateList<DesktopRoute>,
    modifier: Modifier = Modifier,
) {
    val currentRoute = backstack.lastOrNull()

    Box(modifier = modifier.fillMaxSize()) {
        SettingsMainContent(
            onNavigateToBackup = { backstack.add(DesktopRoute.Backup) },
            onNavigateToPlugins = { backstack.add(DesktopRoute.PluginList) },
            onNavigateToAnalytics = { backstack.add(DesktopRoute.Analytics) },
            onNavigateToNotificationCenter = { backstack.add(DesktopRoute.NotificationCenter) },
            onNavigateToBrowsingHistory = { backstack.add(DesktopRoute.BrowsingHistory) },
            onNavigateToDownloadQueue = { backstack.add(DesktopRoute.DownloadQueue) },
        )

        SettingsOverlayContent(backstack, currentRoute)
    }
}

@Composable
@Suppress("LongParameterList")
private fun SettingsMainContent(
    onNavigateToBackup: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToNotificationCenter: () -> Unit,
    onNavigateToBrowsingHistory: () -> Unit,
    onNavigateToDownloadQueue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        val authVm: AuthSettingsViewModel = koinViewModel()
        val displayVm: DisplaySettingsViewModel = koinViewModel()
        val contentFilterVm: ContentFilterSettingsViewModel = koinViewModel()
        val appBehaviorVm: AppBehaviorSettingsViewModel = koinViewModel()
        val storageVm: StorageSettingsViewModel = koinViewModel()
        val updateVm: DesktopUpdateViewModel = koinViewModel()
        DesktopSettingsScreen(
            authSettingsViewModel = authVm,
            displaySettingsViewModel = displayVm,
            contentFilterSettingsViewModel = contentFilterVm,
            appBehaviorSettingsViewModel = appBehaviorVm,
            storageSettingsViewModel = storageVm,
            updateViewModel = updateVm,
            onNavigateToDatasets = { /* Datasets moved to Library tab */ },
            onNavigateToBackup = onNavigateToBackup,
            onNavigateToPlugins = onNavigateToPlugins,
            onNavigateToAnalytics = onNavigateToAnalytics,
            onNavigateToNotificationCenter = onNavigateToNotificationCenter,
            onNavigateToBrowsingHistory = onNavigateToBrowsingHistory,
            onNavigateToDownloadQueue = onNavigateToDownloadQueue,
        )
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun SettingsOverlayContent(
    backstack: SnapshotStateList<DesktopRoute>,
    currentRoute: DesktopRoute?,
) {
    when (currentRoute) {
        is DesktopRoute.Backup -> {
            val vm: DesktopBackupViewModel = koinViewModel()
            DisposableEffect(vm) { onDispose { vm.onCleared() } }
            DesktopBackupScreen(
                viewModel = vm,
                onBack = { backstack.removeLastOrNull() },
            )
        }
        is DesktopRoute.PluginList -> {
            val vm: DesktopPluginViewModel = koinViewModel()
            DisposableEffect(vm) { onDispose { vm.onCleared() } }
            DesktopPluginListScreen(
                viewModel = vm,
                onPluginClick = { pluginId ->
                    backstack.add(DesktopRoute.PluginDetail(pluginId))
                },
                onBack = { backstack.removeLastOrNull() },
            )
        }
        is DesktopRoute.PluginDetail -> {
            val vm: DesktopPluginViewModel = koinViewModel()
            DisposableEffect(vm) { onDispose { vm.onCleared() } }
            DesktopPluginDetailScreen(
                pluginId = currentRoute.pluginId,
                viewModel = vm,
                onBack = { backstack.removeLastOrNull() },
            )
        }
        is DesktopRoute.Analytics -> {
            val vm: DesktopAnalyticsViewModel = koinViewModel()
            DisposableEffect(vm) { onDispose { vm.onCleared() } }
            DesktopAnalyticsScreen(
                viewModel = vm,
                onBack = { backstack.removeLastOrNull() },
            )
        }
        is DesktopRoute.NotificationCenter -> {
            val vm: DesktopNotificationCenterViewModel = koinViewModel()
            DisposableEffect(vm) { onDispose { vm.onCleared() } }
            DesktopNotificationCenterScreen(
                viewModel = vm,
                onBack = { backstack.removeLastOrNull() },
                onModelClick = { modelId ->
                    backstack.add(DesktopRoute.ModelDetail(modelId))
                },
            )
        }
        is DesktopRoute.BrowsingHistory -> {
            val vm: BrowsingHistoryViewModel = koinViewModel()
            DesktopBrowsingHistoryScreen(
                viewModel = vm,
                onBack = { backstack.removeLastOrNull() },
                onModelClick = { modelId ->
                    backstack.add(DesktopRoute.ModelDetail(modelId))
                },
            )
        }
        is DesktopRoute.DownloadQueue -> {
            val vm: DesktopDownloadQueueViewModel = koinViewModel()
            DesktopDownloadQueueScreen(
                viewModel = vm,
                onBack = { backstack.removeLastOrNull() },
            )
        }
        else -> { /* Settings main screen is shown */ }
    }
}
