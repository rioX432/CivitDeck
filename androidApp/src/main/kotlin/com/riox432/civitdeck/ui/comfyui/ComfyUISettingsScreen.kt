package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
import com.riox432.civitdeck.domain.model.ConnectionSecurityLevel
import com.riox432.civitdeck.domain.model.DiscoveredServer
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsUiState
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.ui.theme.CornerRadius
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
) {
    item { StatusSection(state, onTestConnection) }

    // Scan LAN section
    item { ScanLanSection(state, onScanLan, onSelectDiscovered) }

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
            StatusHeader(state, onTest)
            state.testError?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StatusHeader(state: ComfyUISettingsUiState, onTest: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(statusLabel(state.connectionStatus), style = MaterialTheme.typography.titleMedium)
                state.securityLevel?.let { level ->
                    SecurityBadge(level, modifier = Modifier.padding(start = Spacing.sm))
                }
            }
            state.activeConnection?.let {
                Text(it.baseUrl, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (state.isTesting) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else if (state.activeConnection != null) {
            TextButton(onClick = onTest) { Text("Test") }
        }
    }
}

@Composable
private fun SecurityBadge(level: ConnectionSecurityLevel, modifier: Modifier = Modifier) {
    val (icon, color, label) = when (level) {
        ConnectionSecurityLevel.Secure -> Triple("lock", MaterialTheme.colorScheme.primary, "HTTPS")
        ConnectionSecurityLevel.SelfSigned -> Triple("lock", MaterialTheme.colorScheme.tertiary, "Self-signed")
        ConnectionSecurityLevel.LocalInsecure -> Triple("wifi", MaterialTheme.colorScheme.onSurfaceVariant, "LAN")
        ConnectionSecurityLevel.RemoteInsecure -> Triple("warning", MaterialTheme.colorScheme.error, "HTTP")
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

@Composable
private fun ScanLanSection(
    state: ComfyUISettingsUiState,
    onScan: () -> Unit,
    onSelect: (DiscoveredServer) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("LAN Discovery", style = MaterialTheme.typography.titleSmall)
                if (state.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    OutlinedButton(onClick = onScan) { Text("Scan LAN") }
                }
            }
            if (state.discoveredServers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                state.discoveredServers.forEach { server ->
                    DiscoveredServerRow(server, onSelect)
                }
            } else if (!state.isScanning) {
                Text(
                    "Scan your local network for ComfyUI servers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DiscoveredServerRow(server: DiscoveredServer, onSelect: (DiscoveredServer) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(server) }
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(server.displayName, style = MaterialTheme.typography.bodyMedium)
            Text("${server.ip}:${server.port}", style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { onSelect(server) }) { Text("Add") }
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
            .clickable(onClick = onActivate, onClickLabel = "Select connection"),
        shape = RoundedCornerShape(CornerRadius.card),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = isActive, onClick = onActivate)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(connection.name, style = MaterialTheme.typography.titleSmall)
                    if (connection.isSecure) {
                        Text(
                            " HTTPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Text(connection.baseUrl, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddConnectionDialog(
    editing: ComfyUIConnection?,
    onSave: (name: String, hostname: String, port: Int, useHttps: Boolean, acceptSelfSigned: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var hostname by rememberSaveable { mutableStateOf(editing?.hostname ?: "") }
    var portText by rememberSaveable { mutableStateOf(editing?.port?.toString() ?: "8188") }
    var useHttps by rememberSaveable { mutableStateOf(editing?.useHttps ?: false) }
    var acceptSelfSigned by rememberSaveable { mutableStateOf(editing?.acceptSelfSigned ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "Edit Connection" else "Add Connection") },
        text = {
            AddConnectionDialogContent(
                name = name,
                onNameChange = { name = it },
                hostname = hostname,
                onHostnameChange = { hostname = it },
                portText = portText,
                onPortChange = { portText = it },
                useHttps = useHttps,
                onHttpsChange = { useHttps = it },
                acceptSelfSigned = acceptSelfSigned,
                onSelfSignedChange = { acceptSelfSigned = it },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val port = portText.toIntOrNull() ?: ComfyUIConnection.DEFAULT_COMFYUI_PORT
                    onSave(name.ifBlank { hostname }, hostname, port, useHttps, acceptSelfSigned)
                },
                enabled = hostname.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
@Suppress("LongParameterList")
private fun AddConnectionDialogContent(
    name: String,
    onNameChange: (String) -> Unit,
    hostname: String,
    onHostnameChange: (String) -> Unit,
    portText: String,
    onPortChange: (String) -> Unit,
    useHttps: Boolean,
    onHttpsChange: (Boolean) -> Unit,
    acceptSelfSigned: Boolean,
    onSelfSignedChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            placeholder = { Text("e.g. Home PC") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = hostname,
            onValueChange = onHostnameChange,
            label = { Text("Hostname / IP") },
            placeholder = { Text("192.168.1.100") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = portText,
            onValueChange = onPortChange,
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Use HTTPS", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = useHttps, onCheckedChange = onHttpsChange)
        }
        if (useHttps) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Accept self-signed certificates", style = MaterialTheme.typography.bodySmall)
                Switch(checked = acceptSelfSigned, onCheckedChange = onSelfSignedChange)
            }
        }
    }
}

private fun statusLabel(status: ComfyUIConnectionStatus): String = when (status) {
    ComfyUIConnectionStatus.Connected -> "Connected"
    ComfyUIConnectionStatus.Disconnected -> "Disconnected"
    ComfyUIConnectionStatus.Testing -> "Testing..."
    ComfyUIConnectionStatus.Error -> "Connection Error"
    ComfyUIConnectionStatus.NotConfigured -> "No server configured"
}
