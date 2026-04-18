@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.plugin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.presentation.plugin.PluginManagementViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailScreen(
    pluginId: String,
    viewModel: PluginManagementViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val plugin = state.plugins.find { it.id == pluginId }

    LaunchedEffect(pluginId) { viewModel.loadConfig(pluginId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plugin?.name ?: "Plugin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (plugin != null) {
            DetailContent(
                plugin = plugin,
                configJson = state.selectedPluginConfig,
                viewModel = viewModel,
                onBack = onBack,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun DetailContent(
    plugin: InstalledPlugin,
    configJson: String,
    viewModel: PluginManagementViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { Spacer(Modifier.height(Spacing.sm)) }
        item { ManifestSection(plugin) }
        item { HorizontalDivider() }
        item { EnableToggleRow(plugin, viewModel) }
        item { HorizontalDivider() }
        item { CapabilitiesSection(plugin.capabilities) }
        item { HorizontalDivider() }
        item { ConfigSection(plugin.id, configJson, viewModel) }
        item { HorizontalDivider() }
        item { UninstallSection(plugin.id, viewModel, onBack) }
        item { Spacer(Modifier.height(Spacing.lg)) }
    }
}

@Composable
private fun ManifestSection(plugin: InstalledPlugin) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Plugin Info", style = MaterialTheme.typography.titleMedium)
        ManifestRow("Name", plugin.name)
        ManifestRow("Version", plugin.version)
        ManifestRow("Author", plugin.author)
        ManifestRow("Type", plugin.pluginType.name)
        ManifestRow("Min App Version", plugin.minAppVersion)
        Text(
            plugin.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ManifestRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EnableToggleRow(
    plugin: InstalledPlugin,
    viewModel: PluginManagementViewModel,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Enabled", style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = viewModel.isPluginActive(plugin),
            onCheckedChange = { viewModel.togglePlugin(plugin.id, it) },
        )
    }
}

@Composable
private fun CapabilitiesSection(capabilities: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Capabilities", style = MaterialTheme.typography.titleMedium)
        if (capabilities.isEmpty()) {
            Text(
                "No capabilities declared",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            capabilities.forEach { capability ->
                Text(
                    capability,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConfigSection(
    pluginId: String,
    configJson: String,
    viewModel: PluginManagementViewModel,
) {
    var editedConfig by rememberSaveable(configJson) { mutableStateOf(configJson) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Configuration", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = editedConfig,
            onValueChange = { editedConfig = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("JSON Config") },
            minLines = 3,
            maxLines = 8,
        )
        Button(
            onClick = { viewModel.saveConfig(pluginId, editedConfig) },
            enabled = editedConfig != configJson,
        ) {
            Text("Save Config")
        }
    }
}

@Composable
private fun UninstallSection(
    pluginId: String,
    viewModel: PluginManagementViewModel,
    onBack: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }
    Button(
        onClick = { showConfirm = true },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Uninstall Plugin")
    }
    if (showConfirm) {
        UninstallConfirmDialog(
            onConfirm = {
                viewModel.uninstallPlugin(pluginId)
                showConfirm = false
                onBack()
            },
            onDismiss = { showConfirm = false },
        )
    }
}

@Composable
private fun UninstallConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uninstall Plugin") },
        text = { Text("Are you sure? This will remove the plugin and its data.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Uninstall", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
