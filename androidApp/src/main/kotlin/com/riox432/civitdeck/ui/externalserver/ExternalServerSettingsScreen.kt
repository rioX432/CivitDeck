package com.riox432.civitdeck.ui.externalserver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.ExternalServerConfig
import com.riox432.civitdeck.domain.model.ExternalServerConnectionStatus
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsUiState
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalServerSettingsScreen(
    viewModel: ExternalServerSettingsViewModel,
    onBack: () -> Unit,
    onNavigateToGallery: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Server") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onShowAddDialog) {
                Icon(Icons.Default.Add, "Add server")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                ExternalServerStatusCard(
                    state = state,
                    onTest = viewModel::onTestConnection,
                    onNavigateToGallery = onNavigateToGallery,
                )
            }

            if (state.configs.isNotEmpty()) {
                item {
                    Text(
                        text = "Servers",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(state.configs, key = { it.id }) { config ->
                    ExternalServerConfigRow(
                        config = config,
                        isActive = config.id == state.activeConfig?.id,
                        onActivate = { viewModel.onActivateConfig(config.id) },
                        onEdit = { viewModel.onEditConfig(config) },
                        onDelete = { viewModel.onDeleteConfig(config.id) },
                    )
                }
            }
        }
    }

    if (state.showAddDialog) {
        AddServerDialog(
            editing = state.editingConfig,
            onSave = viewModel::onSaveConfig,
            onDismiss = viewModel::onDismissDialog,
        )
    }
}

private fun connectionStatusText(status: ExternalServerConnectionStatus): String = when (status) {
    ExternalServerConnectionStatus.Connected -> "Connected"
    ExternalServerConnectionStatus.Disconnected -> "Not tested"
    ExternalServerConnectionStatus.Testing -> "Testing..."
    ExternalServerConnectionStatus.Error -> "Connection failed"
    ExternalServerConnectionStatus.NotConfigured -> "Not configured"
}

@Composable
private fun ExternalServerStatusCard(
    state: ExternalServerSettingsUiState,
    onTest: () -> Unit,
    onNavigateToGallery: () -> Unit,
) {
    val containerColor = when (state.connectionStatus) {
        ExternalServerConnectionStatus.Connected -> MaterialTheme.colorScheme.primaryContainer
        ExternalServerConnectionStatus.Error -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = state.activeConfig?.name ?: "No server configured",
                style = MaterialTheme.typography.titleMedium,
            )
            val activeConfig = state.activeConfig
            if (activeConfig != null) {
                Text(
                    text = activeConfig.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = connectionStatusText(state.connectionStatus),
                style = MaterialTheme.typography.bodySmall,
            )
            val testError = state.testError
            if (testError != null) {
                Text(
                    text = testError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (state.activeConfig != null) {
                    if (state.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.padding(Spacing.xs))
                    } else {
                        TextButton(onClick = onTest) { Text("Test Connection") }
                    }
                }
                if (state.connectionStatus == ExternalServerConnectionStatus.Connected) {
                    TextButton(onClick = onNavigateToGallery) { Text("Open Gallery") }
                }
            }
        }
    }
}

@Composable
private fun ExternalServerConfigRow(
    config: ExternalServerConfig,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onActivate)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = isActive, onClick = onActivate)
            Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.sm)) {
                Text(text = config.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = config.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete") }
        }
    }
}

@Composable
private fun AddServerDialog(
    editing: ExternalServerConfig?,
    onSave: (name: String, baseUrl: String, apiKey: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var baseUrl by rememberSaveable { mutableStateOf(editing?.baseUrl ?: "") }
    var apiKey by rememberSaveable { mutableStateOf(editing?.apiKey ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing == null) "Add Server" else "Edit Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("http://192.168.1.100:8000/civitdeck") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), baseUrl.trim(), apiKey.trim()) },
                enabled = name.isNotBlank() && baseUrl.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
