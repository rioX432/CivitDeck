package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
import com.riox432.civitdeck.domain.model.DiscoveredServer
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsUiState
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComfyUISettingsScreen(
    viewModel: ComfyUISettingsViewModel,
    onBack: () -> Unit,
    onNavigateToGeneration: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.comfyui_title)) },
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
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onShowAddDialog) {
                Icon(Icons.Default.Add, "Add connection")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            guidedSetupItem(onNavigateToOnboarding)
            settingsItems(
                state = state,
                onTestConnection = viewModel::onTestConnection,
                onNavigateToGeneration = onNavigateToGeneration,
                onNavigateToHistory = onNavigateToHistory,
                onActivate = viewModel::onActivateConnection,
                onEdit = viewModel::onEditConnection,
                onDelete = viewModel::onDeleteConnection,
                onScanLan = viewModel::onScanLan,
                onSelectDiscovered = viewModel::onSelectDiscoveredServer,
                onDismissSuggestion = viewModel::dismissSuggestion,
                onTestNtfy = viewModel::onTestNtfy,
            )
        }
    }

    if (state.showAddDialog) {
        AddConnectionDialog(
            editing = state.editingConnection,
            onSave = { name, hostname, port, https, selfSigned, ntfyUrl, ntfyTopic ->
                viewModel.onSaveConnection(name, hostname, port, https, selfSigned, ntfyUrl, ntfyTopic)
            },
            onDismiss = viewModel::onDismissDialog,
        )
    }
}

private fun LazyListScope.guidedSetupItem(onNavigateToOnboarding: () -> Unit) {
    item {
        androidx.compose.material3.Button(
            onClick = onNavigateToOnboarding,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.comfyui_onboarding_guided_setup))
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.settingsItems(
    state: ComfyUISettingsUiState,
    onTestConnection: () -> Unit,
    onNavigateToGeneration: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onActivate: (Long) -> Unit,
    onEdit: (ComfyUIConnection) -> Unit,
    onDelete: (Long) -> Unit,
    onScanLan: () -> Unit,
    onSelectDiscovered: (DiscoveredServer) -> Unit,
    onDismissSuggestion: (String) -> Unit,
    onTestNtfy: () -> Unit,
) {
    item { StatusSection(state, onTestConnection) }

    // Hardware info section
    state.systemStats?.let { stats ->
        item { ServerHardwareSection(stats) }
    }

    // Optimization suggestions
    val visibleSuggestions = state.optimizationSuggestions
        .filter { it.id !in state.dismissedSuggestionIds }
    if (visibleSuggestions.isNotEmpty()) {
        item {
            OptimizationSuggestionsSection(
                suggestions = visibleSuggestions,
                onDismiss = onDismissSuggestion,
            )
        }
    }

    // ntfy push notifications section
    item { NtfySection(state, onTestNtfy) }

    // Scan LAN section
    item { ScanLanSection(state, onScanLan, onSelectDiscovered) }

    if (state.connectionStatus == ComfyUIConnectionStatus.Connected) {
        item {
            TextButton(onClick = onNavigateToGeneration, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.comfyui_open_txt2img))
            }
        }
        item {
            TextButton(onClick = onNavigateToHistory, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.comfyui_view_gallery))
            }
        }
    }

    items(state.connections, key = { it.id }) { connection ->
        ConnectionCard(
            connection = connection,
            isActive = connection.id == state.activeConnection?.id,
            onActivate = { onActivate(connection.id) },
            onEdit = { onEdit(connection) },
            onDelete = { onDelete(connection.id) },
        )
    }
}
