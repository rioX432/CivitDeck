package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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
import com.riox432.civitdeck.domain.model.SystemStats
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun ComfyUISettingsSection(viewModel: ComfyUISettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "ComfyUI Server") {
        ConnectionStatusBadge(
            label = "ComfyUI",
            status = state.connectionStatus.name,
            isConnected = state.connectionStatus == ComfyUIConnectionStatus.Connected,
        )
        SecurityLevelIndicator(securityLevel = state.securityLevel)
        state.systemStats?.let { stats ->
            Spacer(modifier = Modifier.height(Spacing.sm))
            DesktopServerHardwareSection(stats)
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        DesktopNtfySection(state = state, onTestNtfy = viewModel::onTestNtfy)
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
        AddConnectionFormSection(viewModel)
    }
}

@Composable
private fun AddConnectionFormSection(viewModel: ComfyUISettingsViewModel) {
    var nameInput by remember { mutableStateOf("") }
    var hostInput by remember { mutableStateOf("127.0.0.1") }
    var portInput by remember { mutableStateOf(ComfyUIConnection.DEFAULT_COMFYUI_PORT.toString()) }
    var useHttpsInput by remember { mutableStateOf(false) }
    var ntfyServerUrlInput by remember { mutableStateOf("") }
    var ntfyTopicInput by remember { mutableStateOf("") }

    AddConnectionForm(
        nameInput = nameInput,
        hostInput = hostInput,
        portInput = portInput,
        useHttpsInput = useHttpsInput,
        ntfyServerUrlInput = ntfyServerUrlInput,
        ntfyTopicInput = ntfyTopicInput,
        onNameChanged = { nameInput = it },
        onHostChanged = { hostInput = it },
        onPortChanged = { portInput = it },
        onHttpsChanged = { useHttpsInput = it },
        onNtfyServerUrlChanged = { ntfyServerUrlInput = it },
        onNtfyTopicChanged = { ntfyTopicInput = it },
        onSave = { name, host, port, https, ntfyUrl, ntfyTopic ->
            viewModel.onSaveConnection(name, host, port, https, false, ntfyUrl, ntfyTopic)
            nameInput = ""
            hostInput = "127.0.0.1"
            portInput = ComfyUIConnection.DEFAULT_COMFYUI_PORT.toString()
            useHttpsInput = false
            ntfyServerUrlInput = ""
            ntfyTopicInput = ""
        },
    )
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
private fun DesktopServerHardwareSection(stats: SystemStats) {
    Text("Server Hardware", style = MaterialTheme.typography.labelMedium)
    Spacer(modifier = Modifier.height(Spacing.xs))
    DesktopHardwareRow("GPU", stats.gpuName)
    DesktopVramProgressRow(stats)
    DesktopHardwareRow("RAM", "${stats.ramTotalMB} MB total")
    stats.comfyuiVersion?.let { DesktopHardwareRow("ComfyUI", it) }
    stats.pytorchVersion?.let { DesktopHardwareRow("PyTorch", it) }
    DesktopHardwareRow("OS", stats.os)
}

@Composable
private fun DesktopVramProgressRow(stats: SystemStats) {
    val vramUsed = stats.vramTotalMB - stats.vramFreeMB
    val progress = if (stats.vramTotalMB > 0) vramUsed.toFloat() / stats.vramTotalMB else 0f
    Column {
        Text("VRAM Usage", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(Spacing.xs))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(Spacing.sm),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            "$vramUsed / ${stats.vramTotalMB} MB",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DesktopHardwareRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DesktopNtfySection(
    state: com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsUiState,
    onTestNtfy: () -> Unit,
) {
    Text("Push Notifications (ntfy)", style = MaterialTheme.typography.labelMedium)
    Spacer(modifier = Modifier.height(Spacing.xs))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Status", style = MaterialTheme.typography.labelSmall)
        Text(
            if (state.isNtfySubscribed) "Subscribed" else "Not configured",
            style = MaterialTheme.typography.bodySmall,
            color = if (state.isNtfySubscribed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
    val active = state.activeConnection
    if (active?.isNtfyConfigured == true) {
        Text(
            "${active.resolvedNtfyServerUrl}/${active.ntfyTopic}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedButton(
                onClick = onTestNtfy,
                enabled = !state.isNtfyTestSending,
            ) {
                Text(if (state.isNtfyTestSending) "Sending..." else "Test Notification")
            }
            state.ntfyTestResult?.let { success ->
                Text(
                    if (success) "Sent successfully" else "Failed to send",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (success) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
            }
        }
    } else {
        Text(
            "Configure ntfy topic in your connection to receive push notifications.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun AddConnectionForm(
    nameInput: String,
    hostInput: String,
    portInput: String,
    useHttpsInput: Boolean,
    ntfyServerUrlInput: String,
    ntfyTopicInput: String,
    onNameChanged: (String) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onHttpsChanged: (Boolean) -> Unit,
    onNtfyServerUrlChanged: (String) -> Unit,
    onNtfyTopicChanged: (String) -> Unit,
    onSave: (String, String, Int, Boolean, String?, String?) -> Unit,
) {
    Spacer(modifier = Modifier.height(Spacing.sm))
    Text("Add Connection:", style = MaterialTheme.typography.labelMedium)
    ConnectionInputFields(
        nameInput = nameInput,
        hostInput = hostInput,
        portInput = portInput,
        onNameChanged = onNameChanged,
        onHostChanged = onHostChanged,
        onPortChanged = onPortChanged,
    )
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
    DesktopNtfyFormFields(
        ntfyServerUrlInput = ntfyServerUrlInput,
        ntfyTopicInput = ntfyTopicInput,
        onNtfyServerUrlChanged = onNtfyServerUrlChanged,
        onNtfyTopicChanged = onNtfyTopicChanged,
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    Button(
        onClick = {
            val port = portInput.toIntOrNull() ?: return@Button
            onSave(
                nameInput,
                hostInput,
                port,
                useHttpsInput,
                ntfyServerUrlInput.ifBlank { null },
                ntfyTopicInput.ifBlank { null },
            )
        },
        enabled = nameInput.isNotBlank() && hostInput.isNotBlank(),
    ) {
        Text("Save Connection")
    }
}

@Composable
private fun ConnectionInputFields(
    nameInput: String,
    hostInput: String,
    portInput: String,
    onNameChanged: (String) -> Unit,
    onHostChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
) {
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
}

@Composable
private fun DesktopNtfyFormFields(
    ntfyServerUrlInput: String,
    ntfyTopicInput: String,
    onNtfyServerUrlChanged: (String) -> Unit,
    onNtfyTopicChanged: (String) -> Unit,
) {
    Spacer(modifier = Modifier.height(Spacing.sm))
    Text("ntfy Push Notifications:", style = MaterialTheme.typography.labelMedium)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedTextField(
            value = ntfyServerUrlInput,
            onValueChange = onNtfyServerUrlChanged,
            label = { Text("ntfy Server URL") },
            placeholder = { Text("https://ntfy.sh") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = ntfyTopicInput,
            onValueChange = onNtfyTopicChanged,
            label = { Text("Topic") },
            placeholder = { Text("my-comfyui-topic") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        TextButton(
            onClick = {
                onNtfyTopicChanged(
                    "civitdeck-${java.util.UUID.randomUUID().toString().take(TOPIC_ID_LENGTH)}",
                )
            },
        ) {
            Text("Generate Random Topic")
        }
    }
}

private const val TOPIC_ID_LENGTH = 8
