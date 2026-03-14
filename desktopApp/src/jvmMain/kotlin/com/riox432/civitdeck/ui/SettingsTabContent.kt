package com.riox432.civitdeck

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.riox432.civitdeck.ui.analytics.DesktopAnalyticsViewModel
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

private enum class SettingsSection(val label: String) {
    General("General"),
    ComfyUI("ComfyUI"),
    SDWebUI("SD WebUI"),
    ExternalServer("External Server"),
}

@Composable
fun SettingsTabContent(
    modifier: Modifier = Modifier,
) {
    val authVm: AuthSettingsViewModel = koinViewModel()
    val displayVm: DisplaySettingsViewModel = koinViewModel()
    val contentFilterVm: ContentFilterSettingsViewModel = koinViewModel()
    val appBehaviorVm: AppBehaviorSettingsViewModel = koinViewModel()
    val storageVm: StorageSettingsViewModel = koinViewModel()
    val analyticsVm: DesktopAnalyticsViewModel = koinViewModel()
    val comfySettingsVm: ComfyUISettingsViewModel = koinViewModel()
    val comfyGenVm: ComfyUIGenerationViewModel = koinViewModel()
    val comfyHistoryVm: ComfyUIHistoryViewModel = koinViewModel()
    val sdWebuiSettingsVm: SDWebUISettingsViewModel = koinViewModel()
    val sdWebuiGenVm: SDWebUIGenerationViewModel = koinViewModel()
    val extServerSettingsVm: ExternalServerSettingsViewModel = koinViewModel()
    val extServerGalleryVm: ExternalServerGalleryViewModel = koinViewModel()

    var selectedSection by remember { mutableStateOf(SettingsSection.General) }

    Column(modifier = modifier.fillMaxSize()) {
        SettingsSectionTabs(
            selected = selectedSection,
            onSelected = { selectedSection = it },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            when (selectedSection) {
                SettingsSection.General -> DesktopSettingsScreen(
                    authSettingsViewModel = authVm,
                    displaySettingsViewModel = displayVm,
                    contentFilterSettingsViewModel = contentFilterVm,
                    appBehaviorSettingsViewModel = appBehaviorVm,
                    storageSettingsViewModel = storageVm,
                    analyticsViewModel = analyticsVm,
                )
                SettingsSection.ComfyUI -> {
                    ComfyUISettingsSection(comfySettingsVm)
                    ComfyUIGenerationSection(comfyGenVm)
                    ComfyUIHistorySection(comfyHistoryVm)
                }
                SettingsSection.SDWebUI -> {
                    SDWebUISettingsSection(sdWebuiSettingsVm)
                    SDWebUIGenerationSection(sdWebuiGenVm)
                }
                SettingsSection.ExternalServer -> {
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
) {
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
            SettingsSection.entries.forEach { section ->
                FilterChip(
                    selected = selected == section,
                    onClick = { onSelected(section) },
                    label = { Text(section.label) },
                )
            }
        }
    }
}
