package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.model.ThemeMode
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.StorageSettingsViewModel
import com.riox432.civitdeck.ui.update.DesktopUpdateViewModel
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.Elevation
import java.awt.Desktop as AwtDesktop
import java.net.URI

@Composable
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
        )
        DisplaySettingsSection(displaySettingsViewModel)
        ContentFilterSection(contentFilterSettingsViewModel)
        AppBehaviorSection(appBehaviorSettingsViewModel)
        UpdateSection(updateViewModel)
        StorageSection(storageSettingsViewModel)
    }
}

@Composable
private fun ToolsSection(
    onNavigateToDatasets: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
) {
    SettingsCard(title = "Tools") {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedButton(onClick = onNavigateToDatasets) { Text("Datasets") }
            OutlinedButton(onClick = onNavigateToBackup) { Text("Backup & Restore") }
            OutlinedButton(onClick = onNavigateToPlugins) { Text("Plugins") }
            OutlinedButton(onClick = onNavigateToAnalytics) { Text("Analytics") }
        }
    }
}

// region Auth Settings

@Composable
private fun AuthSettingsSection(viewModel: AuthSettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var apiKeyInput by remember { mutableStateOf("") }

    SettingsCard(title = "Authentication") {
        if (state.connectedUsername != null) {
            Text(
                text = "Connected as: ${state.connectedUsername}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row {
                OutlinedButton(onClick = viewModel::onRefreshUsername) {
                    Text("Refresh")
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                OutlinedButton(onClick = viewModel::onClearApiKey) {
                    Text("Disconnect")
                }
            }
        } else {
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("CivitAI API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = state.apiKeyError != null,
                supportingText = state.apiKeyError?.let { { Text(it) } },
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Button(
                onClick = { viewModel.onValidateAndSaveApiKey(apiKeyInput) },
                enabled = !state.isValidatingApiKey && apiKeyInput.isNotBlank(),
            ) {
                Text(if (state.isValidatingApiKey) "Validating..." else "Connect")
            }
        }
    }
}

// endregion

// region Display Settings

@Composable
private fun DisplaySettingsSection(viewModel: DisplaySettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "Display") {
        SettingsDropdown(
            label = "Theme",
            selected = state.themeMode.name,
            options = ThemeMode.entries.map { it.name },
            onSelected = { viewModel.onThemeModeChanged(ThemeMode.valueOf(it)) },
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        SettingsDropdown(
            label = "Accent Color",
            selected = state.accentColor.displayName,
            options = AccentColor.entries.map { it.displayName },
            onSelected = { name ->
                AccentColor.entries.find { it.displayName == name }
                    ?.let { viewModel.onAccentColorChanged(it) }
            },
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        SliderSetting(
            label = "Grid Columns",
            value = state.gridColumns.toFloat(),
            valueRange = 1f..6f,
            steps = 4,
            valueLabel = state.gridColumns.toString(),
            onValueChange = { viewModel.onGridColumnsChanged(it.toInt()) },
        )
    }
}

// endregion

// region Content Filter

@Composable
private fun ContentFilterSection(viewModel: ContentFilterSettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "Content Filter") {
        SettingsDropdown(
            label = "NSFW Filter",
            selected = state.nsfwFilterLevel.name,
            options = NsfwFilterLevel.entries.map { it.name },
            onSelected = { viewModel.onNsfwFilterChanged(NsfwFilterLevel.valueOf(it)) },
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        SliderSetting(
            label = "Soft Blur Intensity",
            value = state.nsfwBlurSettings.softIntensity.toFloat(),
            valueRange = 0f..100f,
            steps = 9,
            valueLabel = "${state.nsfwBlurSettings.softIntensity}%",
            onValueChange = {
                viewModel.onNsfwBlurSettingsChanged(
                    state.nsfwBlurSettings.copy(softIntensity = it.toInt()),
                )
            },
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        SliderSetting(
            label = "Mature Blur Intensity",
            value = state.nsfwBlurSettings.matureIntensity.toFloat(),
            valueRange = 0f..100f,
            steps = 9,
            valueLabel = "${state.nsfwBlurSettings.matureIntensity}%",
            onValueChange = {
                viewModel.onNsfwBlurSettingsChanged(
                    state.nsfwBlurSettings.copy(matureIntensity = it.toInt()),
                )
            },
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        SliderSetting(
            label = "Explicit Blur Intensity",
            value = state.nsfwBlurSettings.explicitIntensity.toFloat(),
            valueRange = 0f..100f,
            steps = 9,
            valueLabel = "${state.nsfwBlurSettings.explicitIntensity}%",
            onValueChange = {
                viewModel.onNsfwBlurSettingsChanged(
                    state.nsfwBlurSettings.copy(explicitIntensity = it.toInt()),
                )
            },
        )
        if (state.excludedTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text("Excluded Tags:", style = MaterialTheme.typography.labelMedium)
            state.excludedTags.forEach { tag ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tag, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.onRemoveExcludedTag(tag) }) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}

// endregion

// region App Behavior

@Composable
private fun AppBehaviorSection(viewModel: AppBehaviorSettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "App Behavior") {
        SwitchSetting(
            label = "Power User Mode",
            checked = state.powerUserMode,
            onCheckedChange = viewModel::onPowerUserModeChanged,
        )
        SwitchSetting(
            label = "Notifications",
            checked = state.notificationsEnabled,
            onCheckedChange = viewModel::onNotificationsEnabledChanged,
        )
        if (state.notificationsEnabled) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            SettingsDropdown(
                label = "Polling Interval",
                selected = state.pollingInterval.displayName,
                options = PollingInterval.entries.map { it.displayName },
                onSelected = { name ->
                    PollingInterval.entries.find { it.displayName == name }
                        ?.let { viewModel.onPollingIntervalChanged(it) }
                },
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        SliderSetting(
            label = "Feed Quality Threshold",
            value = state.feedQualityThreshold.toFloat(),
            valueRange = 0f..100f,
            steps = 9,
            valueLabel = state.feedQualityThreshold.toString(),
            onValueChange = { viewModel.onFeedQualityThresholdChanged(it.toInt()) },
        )
    }
}

// endregion

// region Update

@Composable
private fun UpdateSection(viewModel: DesktopUpdateViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "Updates") {
        if (state.showBanner && state.updateResult != null) {
            val result = state.updateResult!!
            Text(
                text = "Update available: v${result.currentVersion} \u2192 v${result.latestVersion}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Button(onClick = {
                    if (AwtDesktop.isDesktopSupported()) {
                        AwtDesktop.getDesktop().browse(URI(result.htmlUrl))
                    }
                }) {
                    Text("Download")
                }
                OutlinedButton(onClick = viewModel::dismissBanner) {
                    Text("Dismiss")
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
        }
        SwitchSetting(
            label = "Auto-check for updates",
            checked = state.autoCheckEnabled,
            onCheckedChange = viewModel::setAutoCheckEnabled,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        OutlinedButton(
            onClick = viewModel::checkForUpdate,
            enabled = !state.isChecking,
        ) {
            Text(if (state.isChecking) "Checking..." else "Check now")
        }
    }
}

// endregion

// region Storage

@Composable
private fun StorageSection(viewModel: StorageSettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "Storage & Cache") {
        Text(
            text = if (state.isOnline) "Status: Online" else "Status: Offline",
            style = MaterialTheme.typography.bodySmall,
            color = if (state.isOnline) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
        )
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
            OutlinedButton(onClick = viewModel::onClearCache) { Text("Clear Cache") }
            OutlinedButton(onClick = viewModel::onClearSearchHistory) { Text("Clear Search") }
            OutlinedButton(onClick = viewModel::onClearBrowsingHistory) { Text("Clear History") }
        }
    }
}

// endregion

// region Shared Components

@Composable
internal fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.xs),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(Spacing.md))
            content()
        }
    }
}

@Composable
internal fun SwitchSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
internal fun SettingsDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(Spacing.xs))
        androidx.compose.foundation.layout.Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selected)
            }
            androidx.compose.material3.DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(valueLabel, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
        )
    }
}

// endregion

