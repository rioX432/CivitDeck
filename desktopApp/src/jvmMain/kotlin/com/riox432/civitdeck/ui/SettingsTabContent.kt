package com.riox432.civitdeck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUIGenerationViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerGalleryViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.StorageSettingsViewModel
import com.riox432.civitdeck.ui.DesktopRoute
import com.riox432.civitdeck.util.removeLastOrNull
import com.riox432.civitdeck.ui.analytics.DesktopAnalyticsScreen
import com.riox432.civitdeck.ui.analytics.DesktopAnalyticsViewModel
import com.riox432.civitdeck.ui.backup.DesktopBackupScreen
import com.riox432.civitdeck.ui.backup.DesktopBackupViewModel
import com.riox432.civitdeck.ui.dataset.DesktopDatasetDetailScreen
import com.riox432.civitdeck.ui.dataset.DesktopDatasetDetailViewModel
import com.riox432.civitdeck.ui.dataset.DesktopDatasetListScreen
import com.riox432.civitdeck.ui.dataset.DesktopDatasetListViewModel
import com.riox432.civitdeck.ui.plugin.DesktopPluginDetailScreen
import com.riox432.civitdeck.ui.plugin.DesktopPluginListScreen
import com.riox432.civitdeck.ui.plugin.DesktopPluginViewModel
import com.riox432.civitdeck.ui.settings.ComfyUIGenerationSection
import com.riox432.civitdeck.ui.settings.ComfyUIHistorySection
import com.riox432.civitdeck.ui.settings.ComfyUISettingsSection
import com.riox432.civitdeck.ui.settings.DesktopSettingsScreen
import com.riox432.civitdeck.ui.settings.ExternalServerGallerySection
import com.riox432.civitdeck.ui.settings.ExternalServerSettingsSection
import com.riox432.civitdeck.ui.settings.SDWebUIGenerationSection
import com.riox432.civitdeck.ui.settings.SDWebUISettingsSection
import com.riox432.civitdeck.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private enum class SettingsSection(val label: String) {
    General("General"),
    ComfyUI("ComfyUI"),
    SDWebUI("SD WebUI"),
    ExternalServer("External Server"),
}

@Composable
fun SettingsTabContent(
    backstack: SnapshotStateList<DesktopRoute>,
    modifier: Modifier = Modifier,
) {
    val currentRoute = backstack.lastOrNull()

    Box(modifier = modifier.fillMaxSize()) {
        SettingsMainContent(
            onNavigateToDatasets = { backstack.add(DesktopRoute.DatasetList) },
            onNavigateToBackup = { backstack.add(DesktopRoute.Backup) },
            onNavigateToPlugins = { backstack.add(DesktopRoute.PluginList) },
            onNavigateToAnalytics = { backstack.add(DesktopRoute.Analytics) },
        )

        when (currentRoute) {
            is DesktopRoute.DatasetList -> {
                val vm: DesktopDatasetListViewModel = koinViewModel()
                DesktopDatasetListScreen(
                    viewModel = vm,
                    onDatasetClick = { id, name ->
                        backstack.add(DesktopRoute.DatasetDetail(id, name))
                    },
                    onBack = { backstack.removeLastOrNull() },
                )
            }
            is DesktopRoute.DatasetDetail -> {
                val vm: DesktopDatasetDetailViewModel = koinViewModel(
                    key = "dataset_detail_${currentRoute.datasetId}",
                ) { parametersOf(currentRoute.datasetId) }
                DesktopDatasetDetailScreen(
                    datasetName = currentRoute.datasetName,
                    viewModel = vm,
                    onBack = { backstack.removeLastOrNull() },
                )
            }
            is DesktopRoute.Backup -> {
                val vm: DesktopBackupViewModel = koinViewModel()
                DesktopBackupScreen(
                    viewModel = vm,
                    onBack = { backstack.removeLastOrNull() },
                )
            }
            is DesktopRoute.PluginList -> {
                val vm: DesktopPluginViewModel = koinViewModel()
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
                DesktopPluginDetailScreen(
                    pluginId = currentRoute.pluginId,
                    viewModel = vm,
                    onBack = { backstack.removeLastOrNull() },
                )
            }
            is DesktopRoute.Analytics -> {
                val vm: DesktopAnalyticsViewModel = koinViewModel()
                DesktopAnalyticsScreen(
                    viewModel = vm,
                    onBack = { backstack.removeLastOrNull() },
                )
            }
            else -> { /* Settings main screen is always shown underneath */ }
        }
    }
}

@Composable
private fun SettingsMainContent(
    onNavigateToDatasets: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
) {
    // AppBehaviorVM is always needed (controls power user mode / section visibility)
    val appBehaviorVm: AppBehaviorSettingsViewModel = koinViewModel()
    val appBehaviorState by appBehaviorVm.uiState.collectAsState()
    val isPowerUser = appBehaviorState.powerUserMode

    var selectedSection by remember { mutableStateOf(SettingsSection.General) }
    // Reset to General if current section is hidden by power user mode
    if (!isPowerUser && selectedSection != SettingsSection.General) {
        selectedSection = SettingsSection.General
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsSectionTabs(
            selected = selectedSection,
            onSelected = { selectedSection = it },
            isPowerUser = isPowerUser,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when (selectedSection) {
                SettingsSection.General -> {
                    val authVm: AuthSettingsViewModel = koinViewModel()
                    val displayVm: DisplaySettingsViewModel = koinViewModel()
                    val contentFilterVm: ContentFilterSettingsViewModel = koinViewModel()
                    val storageVm: StorageSettingsViewModel = koinViewModel()
                    DesktopSettingsScreen(
                        authSettingsViewModel = authVm,
                        displaySettingsViewModel = displayVm,
                        contentFilterSettingsViewModel = contentFilterVm,
                        appBehaviorSettingsViewModel = appBehaviorVm,
                        storageSettingsViewModel = storageVm,
                        onNavigateToDatasets = onNavigateToDatasets,
                        onNavigateToBackup = onNavigateToBackup,
                        onNavigateToPlugins = onNavigateToPlugins,
                        onNavigateToAnalytics = onNavigateToAnalytics,
                    )
                }
                SettingsSection.ComfyUI -> {
                    val comfySettingsVm: ComfyUISettingsViewModel = koinViewModel()
                    val comfyGenVm: ComfyUIGenerationViewModel = koinViewModel()
                    val comfyHistoryVm: ComfyUIHistoryViewModel = koinViewModel()
                    ComfyUISettingsSection(comfySettingsVm)
                    ComfyUIGenerationSection(comfyGenVm)
                    ComfyUIHistorySection(comfyHistoryVm)
                }
                SettingsSection.SDWebUI -> {
                    val sdWebuiSettingsVm: SDWebUISettingsViewModel = koinViewModel()
                    val sdWebuiGenVm: SDWebUIGenerationViewModel = koinViewModel()
                    SDWebUISettingsSection(sdWebuiSettingsVm)
                    SDWebUIGenerationSection(sdWebuiGenVm)
                }
                SettingsSection.ExternalServer -> {
                    val extServerSettingsVm: ExternalServerSettingsViewModel = koinViewModel()
                    val extServerGalleryVm: ExternalServerGalleryViewModel = koinViewModel()
                    ExternalServerSettingsSection(extServerSettingsVm)
                    ExternalServerGallerySection(extServerGalleryVm)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTabs(
    selected: SettingsSection,
    onSelected: (SettingsSection) -> Unit,
    isPowerUser: Boolean,
) {
    val visibleSections = if (isPowerUser) {
        SettingsSection.entries
    } else {
        listOf(SettingsSection.General)
    }

    Surface(tonalElevation = 1.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = Spacing.md),
            )
            visibleSections.forEach { section ->
                FilterChip(
                    selected = selected == section,
                    onClick = { onSelected(section) },
                    label = { Text(section.label) },
                )
            }
        }
    }
}
