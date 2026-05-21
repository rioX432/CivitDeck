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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.LinearProgressIndicator
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
import com.riox432.civitdeck.domain.model.SystemStats
import com.riox432.civitdeck.domain.util.OptimizationSuggestion
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
            TextButton(onClick = onTest) { Text(stringResource(R.string.action_test)) }
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
private fun ServerHardwareSection(stats: SystemStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                stringResource(R.string.comfyui_server_hardware),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            HardwareGpuRow(stats)
            VramProgressRow(stats)
            HardwareInfoRows(stats)
        }
    }
}

@Composable
private fun HardwareGpuRow(stats: SystemStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.comfyui_gpu), style = MaterialTheme.typography.labelMedium)
        Text(stats.gpuName, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun VramProgressRow(stats: SystemStats) {
    val vramUsed = stats.vramTotalMB - stats.vramFreeMB
    val progress = if (stats.vramTotalMB > 0) vramUsed.toFloat() / stats.vramTotalMB else 0f
    Spacer(modifier = Modifier.height(Spacing.xs))
    Text(stringResource(R.string.comfyui_vram_usage), style = MaterialTheme.typography.labelMedium)
    Spacer(modifier = Modifier.height(Spacing.xs))
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(Spacing.sm),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
    Text(
        stringResource(R.string.comfyui_vram_format, vramUsed, stats.vramTotalMB),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun HardwareInfoRows(stats: SystemStats) {
    Spacer(modifier = Modifier.height(Spacing.xs))
    HardwareInfoRow(stringResource(R.string.comfyui_ram), stringResource(R.string.comfyui_ram_format, stats.ramTotalMB))
    stats.comfyuiVersion?.let { HardwareInfoRow(stringResource(R.string.comfyui_comfyui_version), it) }
    stats.pytorchVersion?.let { HardwareInfoRow(stringResource(R.string.comfyui_pytorch_version), it) }
    HardwareInfoRow(stringResource(R.string.comfyui_os), stats.os)
}

@Composable
private fun HardwareInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun OptimizationSuggestionsSection(
    suggestions: List<OptimizationSuggestion>,
    onDismiss: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                stringResource(R.string.vram_optimization_suggestions),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            suggestions.forEach { suggestion ->
                SuggestionCard(suggestion = suggestion, onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: OptimizationSuggestion,
    onDismiss: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(CornerRadius.card),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    suggestion.title,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onDismiss(suggestion.id) }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_dismiss),
                    modifier = Modifier.size(Spacing.lg),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NtfySection(
    state: ComfyUISettingsUiState,
    onTestNtfy: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            NtfySectionHeader(state)
            Spacer(modifier = Modifier.height(Spacing.sm))
            NtfySectionContent(state, onTestNtfy)
        }
    }
}

@Composable
private fun NtfySectionHeader(state: ComfyUISettingsUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.ntfy_section_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = if (state.isNtfySubscribed) {
                stringResource(R.string.ntfy_status_subscribed)
            } else {
                stringResource(R.string.ntfy_status_not_configured)
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (state.isNtfySubscribed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun NtfySectionContent(
    state: ComfyUISettingsUiState,
    onTestNtfy: () -> Unit,
) {
    val active = state.activeConnection
    if (active?.isNtfyConfigured == true) {
        Text(
            "${active.resolvedNtfyServerUrl}/${active.ntfyTopic}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        NtfyTestButton(state, onTestNtfy)
    } else {
        Text(
            stringResource(R.string.ntfy_setup_guide),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NtfyTestButton(
    state: ComfyUISettingsUiState,
    onTestNtfy: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedButton(
            onClick = onTestNtfy,
            enabled = !state.isNtfyTestSending,
        ) {
            if (state.isNtfyTestSending) {
                CircularProgressIndicator(modifier = Modifier.size(Spacing.lg))
            } else {
                Text(stringResource(R.string.ntfy_test_notification))
            }
        }
        state.ntfyTestResult?.let { success ->
            Text(
                text = stringResource(
                    if (success) R.string.ntfy_test_success else R.string.ntfy_test_failed,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (success) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
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
                Text(stringResource(R.string.comfyui_lan_discovery), style = MaterialTheme.typography.titleSmall)
                if (state.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    OutlinedButton(onClick = onScan) { Text(stringResource(R.string.comfyui_scan_lan)) }
                }
            }
            if (state.discoveredServers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                state.discoveredServers.forEach { server ->
                    DiscoveredServerRow(server, onSelect)
                }
            } else if (!state.isScanning) {
                Text(
                    stringResource(R.string.comfyui_lan_scan_hint),
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
        TextButton(onClick = { onSelect(server) }) { Text(stringResource(R.string.action_add)) }
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
@Suppress("LongMethod")
private fun AddConnectionDialog(
    editing: ComfyUIConnection?,
    onSave: (
        name: String,
        hostname: String,
        port: Int,
        useHttps: Boolean,
        acceptSelfSigned: Boolean,
        ntfyServerUrl: String?,
        ntfyTopic: String?,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var hostname by rememberSaveable { mutableStateOf(editing?.hostname ?: "") }
    var portText by rememberSaveable { mutableStateOf(editing?.port?.toString() ?: "8188") }
    var useHttps by rememberSaveable { mutableStateOf(editing?.useHttps ?: false) }
    var acceptSelfSigned by rememberSaveable { mutableStateOf(editing?.acceptSelfSigned ?: false) }
    var ntfyServerUrl by rememberSaveable {
        mutableStateOf(editing?.ntfyServerUrl ?: "")
    }
    var ntfyTopic by rememberSaveable { mutableStateOf(editing?.ntfyTopic ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (editing != null) R.string.comfyui_edit_connection else R.string.comfyui_add_connection
                )
            )
        },
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
                ntfyServerUrl = ntfyServerUrl,
                onNtfyServerUrlChange = { ntfyServerUrl = it },
                ntfyTopic = ntfyTopic,
                onNtfyTopicChange = { ntfyTopic = it },
                onGenerateNtfyTopic = {
                    ntfyTopic = "civitdeck-${java.util.UUID.randomUUID().toString().take(TOPIC_ID_LENGTH)}"
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val port = portText.toIntOrNull() ?: ComfyUIConnection.DEFAULT_COMFYUI_PORT
                    onSave(
                        name.ifBlank { hostname },
                        hostname,
                        port,
                        useHttps,
                        acceptSelfSigned,
                        ntfyServerUrl.ifBlank { null },
                        ntfyTopic.ifBlank { null },
                    )
                },
                enabled = hostname.isNotBlank(),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private const val TOPIC_ID_LENGTH = 8

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
    ntfyServerUrl: String,
    onNtfyServerUrlChange: (String) -> Unit,
    ntfyTopic: String,
    onNtfyTopicChange: (String) -> Unit,
    onGenerateNtfyTopic: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ConnectionFields(name, onNameChange, hostname, onHostnameChange, portText, onPortChange)
        SecurityToggles(useHttps, onHttpsChange, acceptSelfSigned, onSelfSignedChange)
        NtfyFields(ntfyServerUrl, onNtfyServerUrlChange, ntfyTopic, onNtfyTopicChange, onGenerateNtfyTopic)
    }
}

@Composable
@Suppress("LongParameterList")
private fun ConnectionFields(
    name: String,
    onNameChange: (String) -> Unit,
    hostname: String,
    onHostnameChange: (String) -> Unit,
    portText: String,
    onPortChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.label_name)) },
        placeholder = { Text(stringResource(R.string.comfyui_name_placeholder)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = hostname,
        onValueChange = onHostnameChange,
        label = { Text(stringResource(R.string.comfyui_hostname_label)) },
        placeholder = { Text(stringResource(R.string.comfyui_hostname_placeholder)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = portText,
        onValueChange = onPortChange,
        label = { Text(stringResource(R.string.label_port)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SecurityToggles(
    useHttps: Boolean,
    onHttpsChange: (Boolean) -> Unit,
    acceptSelfSigned: Boolean,
    onSelfSignedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.comfyui_use_https), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = useHttps, onCheckedChange = onHttpsChange)
    }
    if (useHttps) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.comfyui_accept_self_signed), style = MaterialTheme.typography.bodySmall)
            Switch(checked = acceptSelfSigned, onCheckedChange = onSelfSignedChange)
        }
    }
}

@Composable
private fun NtfyFields(
    ntfyServerUrl: String,
    onNtfyServerUrlChange: (String) -> Unit,
    ntfyTopic: String,
    onNtfyTopicChange: (String) -> Unit,
    onGenerateNtfyTopic: () -> Unit,
) {
    Spacer(modifier = Modifier.height(Spacing.sm))
    Text(
        stringResource(R.string.ntfy_section_title),
        style = MaterialTheme.typography.titleSmall,
    )
    OutlinedTextField(
        value = ntfyServerUrl,
        onValueChange = onNtfyServerUrlChange,
        label = { Text(stringResource(R.string.ntfy_server_url_label)) },
        placeholder = { Text(stringResource(R.string.ntfy_server_url_placeholder)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = ntfyTopic,
        onValueChange = onNtfyTopicChange,
        label = { Text(stringResource(R.string.ntfy_topic_label)) },
        placeholder = { Text(stringResource(R.string.ntfy_topic_placeholder)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    TextButton(onClick = onGenerateNtfyTopic) {
        Text(stringResource(R.string.ntfy_generate_topic))
    }
    Text(
        stringResource(R.string.ntfy_setup_guide),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun statusLabel(status: ComfyUIConnectionStatus): String = when (status) {
    ComfyUIConnectionStatus.Connected -> stringResource(R.string.comfyui_status_connected)
    ComfyUIConnectionStatus.Disconnected -> stringResource(R.string.comfyui_status_disconnected)
    ComfyUIConnectionStatus.Testing -> stringResource(R.string.comfyui_status_testing)
    ComfyUIConnectionStatus.Error -> stringResource(R.string.comfyui_status_error)
    ComfyUIConnectionStatus.NotConfigured -> stringResource(R.string.comfyui_status_not_configured)
}
