package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
import com.riox432.civitdeck.domain.model.ConnectionSecurityLevel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun ComfyUISettingsSection(viewModel: ComfyUISettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var nameInput by remember { mutableStateOf("") }
    var hostInput by remember { mutableStateOf("127.0.0.1") }
    var portInput by remember { mutableStateOf(ComfyUIConnection.DEFAULT_COMFYUI_PORT.toString()) }
    var useHttpsInput by remember { mutableStateOf(false) }

    SettingsCard(title = "ComfyUI Server") {
        ConnectionStatusBadge(
            label = "ComfyUI",
            status = state.connectionStatus.name,
            isConnected = state.connectionStatus == ComfyUIConnectionStatus.Connected,
        )
        SecurityLevelIndicator(securityLevel = state.securityLevel)
        Spacer(modifier = Modifier.height(Spacing.sm))
        LanScanSection(
            isScanning = state.isScanning,
            discoveredServers = state.discoveredServers,
            onScanLan = viewModel::onScanLan,
            onSelectServer = viewModel::onSelectDiscoveredServer,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        ActiveConnectionSection(
            activeConnection = state.activeConnection,
            isTesting = state.isTesting,
            testError = state.testError,
            onTestConnection = viewModel::onTestConnection,
        )
        SavedConnectionsList(
            connections = state.connections,
            onActivate = { viewModel.onActivateConnection(it) },
            onDelete = { viewModel.onDeleteConnection(it) },
        )
        AddConnectionForm(
            nameInput = nameInput,
            hostInput = hostInput,
            portInput = portInput,
            useHttpsInput = useHttpsInput,
            onNameChanged = { nameInput = it },
            onHostChanged = { hostInput = it },
            onPortChanged = { portInput = it },
            onHttpsChanged = { useHttpsInput = it },
            onSave = { name, host, port, https ->
                viewModel.onSaveConnection(name, host, port, https, false)
                nameInput = ""
                hostInput = "127.0.0.1"
                portInput = ComfyUIConnection.DEFAULT_COMFYUI_PORT.toString()
                useHttpsInput = false
            },
        )
    }
}

@Composable
private fun SecurityLevelIndicator(
    securityLevel: ConnectionSecurityLevel?,
) {
    securityLevel?.let { level ->
        Text(
            text = when (level) {
                ConnectionSecurityLevel.Secure -> "Secure (HTTPS)"
                ConnectionSecurityLevel.SelfSigned -> "Self-signed certificate"
                ConnectionSecurityLevel.LocalInsecure -> "LAN (HTTP)"
                ConnectionSecurityLevel.RemoteInsecure -> "Warning: HTTP over internet"
            },
            style = MaterialTheme.typography.labelSmall,
            color = when (level) {
                ConnectionSecurityLevel.Secure ->
                    MaterialTheme.colorScheme.primary
                ConnectionSecurityLevel.RemoteInsecure ->
                    MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun LanScanSection(
    isScanning: Boolean,
    discoveredServers: List<com.riox432.civitdeck.domain.model.DiscoveredServer>,
    onScanLan: () -> Unit,
    onSelectServer: (com.riox432.civitdeck.domain.model.DiscoveredServer) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedButton(
            onClick = onScanLan,
            enabled = !isScanning,
        ) {
            Text(if (isScanning) "Scanning..." else "Scan LAN")
        }
    }
    if (discoveredServers.isNotEmpty()) {
        Spacer(modifier = Modifier.height(Spacing.xs))
        discoveredServers.forEach { server ->
            DiscoveredServerRow(server = server, onSelect = { onSelectServer(server) })
        }
    }
}

@Composable
private fun DiscoveredServerRow(
    server: com.riox432.civitdeck.domain.model.DiscoveredServer,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${server.displayName} (${server.ip}:${server.port})",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSelect) {
            Text("Add")
        }
    }
}

@Composable
private fun ActiveConnectionSection(
    activeConnection: ComfyUIConnection?,
    isTesting: Boolean,
    testError: String?,
    onTestConnection: () -> Unit,
) {
    activeConnection?.let { active ->
        Text(
            text = "Active: ${active.name} (${active.baseUrl})",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedButton(
                onClick = onTestConnection,
                enabled = !isTesting,
            ) {
                Text(if (isTesting) "Testing..." else "Test")
            }
        }
        testError?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SavedConnectionsList(
    connections: List<ComfyUIConnection>,
    onActivate: (Long) -> Unit,
    onDelete: (Long) -> Unit,
) {
    if (connections.isNotEmpty()) {
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text("Saved Connections:", style = MaterialTheme.typography.labelMedium)
        connections.forEach { conn ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${conn.name} (${conn.baseUrl})",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Row {
                    TextButton(onClick = { onActivate(conn.id) }) {
                        Text("Activate")
                    }
                    TextButton(onClick = { onDelete(conn.id) }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddConnectionForm(
    nameInput: String,
    hostInput: String,
    portInput: String,
    useHttpsInput: Boolean,
    onNameChanged: (String) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onHttpsChanged: (Boolean) -> Unit,
    onSave: (String, String, Int, Boolean) -> Unit,
) {
    Spacer(modifier = Modifier.height(Spacing.sm))
    Text("Add Connection:", style = MaterialTheme.typography.labelMedium)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = nameInput,
            onValueChange = onNameChanged,
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = hostInput,
            onValueChange = onHostChanged,
            label = { Text("Host") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = portInput,
            onValueChange = onPortChanged,
            label = { Text("Port") },
            singleLine = true,
            modifier = Modifier.width(Spacing.xxl * 3),
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        androidx.compose.material3.Checkbox(
            checked = useHttpsInput,
            onCheckedChange = onHttpsChanged,
        )
        Text("Use HTTPS", style = MaterialTheme.typography.bodySmall)
    }
    Spacer(modifier = Modifier.height(Spacing.sm))
    Button(
        onClick = {
            val port = portInput.toIntOrNull() ?: return@Button
            onSave(nameInput, hostInput, port, useHttpsInput)
        },
        enabled = nameInput.isNotBlank() && hostInput.isNotBlank(),
    ) {
        Text("Save Connection")
    }
}
