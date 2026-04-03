package com.riox432.civitdeck.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.presentation.backup.BackupViewModel
import com.riox432.civitdeck.presentation.modelfiles.ModelFileBrowserViewModel
import com.riox432.civitdeck.presentation.plugin.PluginManagementViewModel
import com.riox432.civitdeck.presentation.settings.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.presentation.settings.ContentFilterSettingsViewModel
import com.riox432.civitdeck.presentation.settings.DisplaySettingsViewModel
import com.riox432.civitdeck.presentation.settings.StorageSettingsViewModel
import com.riox432.civitdeck.ui.backup.BackupScreen
import com.riox432.civitdeck.ui.modelfiles.ModelFileBrowserScreen
import com.riox432.civitdeck.ui.plugin.PluginDetailScreen
import com.riox432.civitdeck.ui.plugin.PluginManagementScreen
import com.riox432.civitdeck.ui.settings.AdvancedSettingsScreen
import com.riox432.civitdeck.ui.settings.AppearanceSettingsScreen
import com.riox432.civitdeck.ui.settings.ContentFilterSettingsScreen
import com.riox432.civitdeck.ui.settings.IntegrationsHubScreen
import com.riox432.civitdeck.ui.settings.LicensesScreen
import com.riox432.civitdeck.ui.settings.NavShortcutsSettingsScreen
import com.riox432.civitdeck.ui.settings.StorageSettingsScreen
import org.koin.compose.viewmodel.koinViewModel

internal fun EntryProviderScope<Any>.settingsSubScreenEntries(backStack: MutableList<Any>) {
    settingsDisplayEntries(backStack)
    settingsBehaviorEntries(backStack)
    entry<BackupRoute> {
        val viewModel: BackupViewModel = koinViewModel()
        BackupScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<LicensesRoute> {
        LicensesScreen(onBack = { backStack.removeLastOrNull() })
    }
    entry<ModelFileBrowserRoute> {
        val viewModel: ModelFileBrowserViewModel = koinViewModel()
        ModelFileBrowserScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
    pluginEntries(backStack)
}

private fun EntryProviderScope<Any>.settingsDisplayEntries(backStack: MutableList<Any>) {
    entry<AppearanceSettingsRoute> {
        val viewModel: DisplaySettingsViewModel = koinViewModel()
        AppearanceSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<ContentFilterSettingsRoute> {
        val viewModel: ContentFilterSettingsViewModel = koinViewModel()
        val displayVm: DisplaySettingsViewModel = koinViewModel()
        val behaviorVm: AppBehaviorSettingsViewModel = koinViewModel()
        ContentFilterSettingsScreen(
            viewModel = viewModel,
            displayViewModel = displayVm,
            appBehaviorViewModel = behaviorVm,
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<NavShortcutsSettingsRoute> {
        val viewModel: DisplaySettingsViewModel = koinViewModel()
        NavShortcutsSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}

private fun EntryProviderScope<Any>.settingsBehaviorEntries(backStack: MutableList<Any>) {
    entry<StorageSettingsRoute> {
        val viewModel: StorageSettingsViewModel = koinViewModel()
        StorageSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onNavigateToBackup = { backStack.add(BackupRoute) },
        )
    }
    entry<AdvancedSettingsRoute> {
        val viewModel: AppBehaviorSettingsViewModel = koinViewModel()
        val historyVm: ComfyUIHistoryViewModel = koinViewModel()
        val shareHashtags by historyVm.shareHashtags.collectAsStateWithLifecycle()
        AdvancedSettingsScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onNavigateToIntegrations = { backStack.add(IntegrationsHubRoute) },
            onNavigateToModelFiles = { backStack.add(ModelFileBrowserRoute) },
            onNavigateToPlugins = { backStack.add(PluginManagementRoute) },
            onNavigateToNavShortcuts = { backStack.add(NavShortcutsSettingsRoute) },
            shareHashtags = shareHashtags,
            onToggleShareHashtag = historyVm::onToggleShareHashtag,
            onAddShareHashtag = historyVm::onAddShareHashtag,
            onRemoveShareHashtag = historyVm::onRemoveShareHashtag,
        )
    }
    entry<IntegrationsHubRoute> {
        IntegrationsHubScreen(
            onBack = { backStack.removeLastOrNull() },
            onNavigateToComfyUI = { backStack.add(ComfyUISettingsRoute) },
            onNavigateToTemplates = { backStack.add(WorkflowTemplateLibraryRoute) },
            onNavigateToComfyHub = { backStack.add(ComfyHubBrowserRoute) },
            onNavigateToSDWebUI = { backStack.add(SDWebUISettingsRoute) },
            onNavigateToCivitaiLink = { backStack.add(CivitaiLinkSettingsRoute) },
            onNavigateToExternalServer = { backStack.add(ExternalServerSettingsRoute) },
        )
    }
}

private fun EntryProviderScope<Any>.pluginEntries(backStack: MutableList<Any>) {
    entry<PluginManagementRoute> {
        val viewModel: PluginManagementViewModel = koinViewModel()
        PluginManagementScreen(
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
            onPluginClick = { pluginId -> backStack.add(PluginDetailRoute(pluginId)) },
        )
    }
    entry<PluginDetailRoute> { key ->
        val viewModel: PluginManagementViewModel = koinViewModel()
        PluginDetailScreen(
            pluginId = key.pluginId,
            viewModel = viewModel,
            onBack = { backStack.removeLastOrNull() },
        )
    }
}
