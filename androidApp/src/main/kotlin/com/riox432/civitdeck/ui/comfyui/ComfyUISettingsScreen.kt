package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
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
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ComfyUI") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            settingsItems(
                state = state,
                onTestConnection = viewModel::onTestConnection,
                onNavigateToGeneration = onNavigateToGeneration,
                onNavigateToHistory = onNavigateToHistory,
                onActivate = viewModel::onActivateConnection,
                onEdit = viewModel::onEditConnection,
                onDelete = viewModel::onDeleteConnection,
            )
        }
    }

    if (state.showAddDialog) {
        AddConnectionDialog(
            editing = state.editingConnection,
            onSave = viewModel::onSaveConnection,
            onDismiss = viewModel::onDismissDialog,
        )
    }
}

private fun LazyListScope.settingsItems(
    state: ComfyUISettingsUiState,
    onTestConnection: () -> Unit,
    onNavigateToGeneration: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onActivate: (Long) -> Unit,
    onEdit: (ComfyUIConnection) -> Unit,
    onDelete: (Long) -> Unit,
) {
    item { StatusSection(state, onTestConnection) }

    if (state.connectionStatus == ComfyUIConnectionStatus.Connected) {
        item {
            TextButton(onClick = onNavigateToGeneration, modifier = Modifier.fillMaxWidth()) {
                Text("Open txt2img Generator")
            }
        }
        item {
            TextButton(onClick = onNavigateToHistory, modifier = Modifier.fillMaxWidth()) {
                Text("View Output Gallery")
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

@Composable
private fun StatusSection(
    state: ComfyUISettingsUiState,
    onTest: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state.connectionStatus) {
                ComfyUIConnectionStatus.Connected ->
                    MaterialTheme.colorScheme.primaryContainer

                ComfyUIConnectionStatus.Error ->
                    MaterialTheme.colorScheme.errorContainer

                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        statusLabel(state.connectionStatus),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    state.activeConnection?.let {
                        Text(
                            "${it.hostname}:${it.port}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (state.isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else if (state.activeConnection != null) {
                    TextButton(onClick = onTest) { Text("Test") }
                }
            }
            state.testError?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    connection: ComfyUIConnection,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onActivate),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = isActive, onClick = onActivate)
            Column(modifier = Modifier.weight(1f)) {
                Text(connection.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${connection.hostname}:${connection.port}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AddConnectionDialog(
    editing: ComfyUIConnection?,
    onSave: (name: String, hostname: String, port: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var hostname by rememberSaveable {
        mutableStateOf(editing?.hostname ?: "")
    }
    var portText by rememberSaveable {
        mutableStateOf(editing?.port?.toString() ?: "8188")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editing != null) "Edit Connection" else "Add Connection")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Home PC") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = hostname,
                    onValueChange = { hostname = it },
                    label = { Text("Hostname / IP") },
                    placeholder = { Text("192.168.1.100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val port = portText.toIntOrNull() ?: 8188
                    onSave(name.ifBlank { hostname }, hostname, port)
                },
                enabled = hostname.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun statusLabel(status: ComfyUIConnectionStatus): String = when (status) {
    ComfyUIConnectionStatus.Connected -> "Connected"
    ComfyUIConnectionStatus.Disconnected -> "Disconnected"
    ComfyUIConnectionStatus.Testing -> "Testing..."
    ComfyUIConnectionStatus.Error -> "Connection Error"
    ComfyUIConnectionStatus.NotConfigured -> "No server configured"
}
