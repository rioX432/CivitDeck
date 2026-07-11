package com.riox432.civitdeck.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
import com.riox432.civitdeck.domain.model.ExternalServerConnectionStatus
import com.riox432.civitdeck.domain.model.SDWebUIConnectionStatus
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsViewModel
import com.riox432.civitdeck.ui.desktopFocusRing
import com.riox432.civitdeck.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

private enum class HubStatus { Connected, Error, Unknown }

/**
 * Create hub with live per-server connection status (parity with the
 * Android/iOS Create hub): configured servers lead with a status card,
 * unconfigured ones collapse into "Connect" rows.
 */
@Composable
fun DesktopCreateHubScreen(
    onComfyUIClick: () -> Unit,
    onSDWebUIClick: () -> Unit,
    onExternalServerClick: () -> Unit,
    onWorkflowTemplatesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val comfyVm: ComfyUISettingsViewModel = koinViewModel()
    val sdVm: SDWebUISettingsViewModel = koinViewModel()
    val externalVm: ExternalServerSettingsViewModel = koinViewModel()
    val comfyState by comfyVm.uiState.collectAsState()
    val sdState by sdVm.uiState.collectAsState()
    val externalState by externalVm.uiState.collectAsState()

    val comfyConfigured = comfyState.activeConnection != null
    val sdConfigured = sdState.activeConnection != null
    val externalConfigured = externalState.activeConfig != null
    val nothingConfigured = !comfyConfigured && !sdConfigured && !externalConfigured

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = "Create",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )
        if (nothingConfigured) {
            ConnectServerHero(onSetUp = onComfyUIClick)
        }
        if (comfyConfigured) {
            ComfyUIServerCard(comfyState, onComfyUIClick)
        }
        if (sdConfigured) {
            SDWebUIServerCard(sdState, onSDWebUIClick)
        }
        if (externalConfigured) {
            ExternalServerCard(externalState, onExternalServerClick)
        }
        ConnectMoreSection(
            comfyConfigured = comfyConfigured,
            sdConfigured = sdConfigured,
            externalConfigured = externalConfigured,
            nothingConfigured = nothingConfigured,
            onComfyUIClick = onComfyUIClick,
            onSDWebUIClick = onSDWebUIClick,
            onExternalServerClick = onExternalServerClick,
        )
        UtilityRow(
            icon = Icons.Default.Description,
            title = "Workflow Templates",
            subtitle = "Manage and use workflow templates",
            onClick = onWorkflowTemplatesClick,
        )
    }
}

@Composable
private fun ComfyUIServerCard(
    state: com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsUiState,
    onClick: () -> Unit,
) {
    ServerCard(
        icon = Icons.Default.Brush,
        title = "ComfyUI",
        subtitle = state.activeConnection?.let { "${it.hostname}:${it.port}" }.orEmpty(),
        status = state.connectionStatus.toHubStatus(),
        onClick = onClick,
    )
}

@Composable
private fun SDWebUIServerCard(
    state: com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsUiState,
    onClick: () -> Unit,
) {
    ServerCard(
        icon = Icons.Default.Cloud,
        title = "SD WebUI",
        subtitle = state.activeConnection?.let { "${it.hostname}:${it.port}" }.orEmpty(),
        status = state.connectionStatus.toHubStatus(),
        onClick = onClick,
    )
}

@Composable
private fun ExternalServerCard(
    state: com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsUiState,
    onClick: () -> Unit,
) {
    ServerCard(
        icon = Icons.Default.Dns,
        title = state.activeConfig?.name ?: "External Server",
        subtitle = state.activeConfig?.baseUrl.orEmpty(),
        status = state.connectionStatus.toHubStatus(),
        onClick = onClick,
    )
}

@Composable
@Suppress("LongParameterList")
private fun ConnectMoreSection(
    comfyConfigured: Boolean,
    sdConfigured: Boolean,
    externalConfigured: Boolean,
    nothingConfigured: Boolean,
    onComfyUIClick: () -> Unit,
    onSDWebUIClick: () -> Unit,
    onExternalServerClick: () -> Unit,
) {
    // The hero already offers ComfyUI setup when nothing is configured.
    val showComfy = !comfyConfigured && !nothingConfigured
    if (!showComfy && sdConfigured && externalConfigured) return
    Text(
        text = "Connect",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.sm),
    )
    if (showComfy) {
        UtilityRow(Icons.Default.Brush, "ComfyUI", null, onComfyUIClick)
    }
    if (!sdConfigured) {
        UtilityRow(Icons.Default.Cloud, "SD WebUI", null, onSDWebUIClick)
    }
    if (!externalConfigured) {
        UtilityRow(Icons.Default.Dns, "External Server", null, onExternalServerClick)
    }
}

@Composable
private fun ConnectServerHero(onSetUp: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Connect a generation server",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Control ComfyUI or SD WebUI on your PC from CivitDeck",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onSetUp) {
                Text("Set up ComfyUI")
            }
        }
    }
}

@Composable
private fun ServerCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    status: HubStatus,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .desktopFocusRing()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            StatusDot(status)
        }
    }
}

@Composable
private fun StatusDot(status: HubStatus) {
    val (color, label) = when (status) {
        HubStatus.Connected -> Color(GREEN_DOT) to "Online"
        HubStatus.Error -> MaterialTheme.colorScheme.error to "Offline"
        HubStatus.Unknown -> MaterialTheme.colorScheme.outline to "Not tested"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UtilityRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .desktopFocusRing()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xs, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// "Disconnected" means "saved but not tested yet", not a failed test — map to Unknown.
private fun ComfyUIConnectionStatus.toHubStatus(): HubStatus = when (this) {
    ComfyUIConnectionStatus.Connected -> HubStatus.Connected
    ComfyUIConnectionStatus.Error -> HubStatus.Error
    else -> HubStatus.Unknown
}

private fun SDWebUIConnectionStatus.toHubStatus(): HubStatus = when (this) {
    SDWebUIConnectionStatus.Connected -> HubStatus.Connected
    SDWebUIConnectionStatus.Error -> HubStatus.Error
    else -> HubStatus.Unknown
}

private fun ExternalServerConnectionStatus.toHubStatus(): HubStatus = when (this) {
    ExternalServerConnectionStatus.Connected -> HubStatus.Connected
    ExternalServerConnectionStatus.Error -> HubStatus.Error
    else -> HubStatus.Unknown
}

private const val GREEN_DOT = 0xFF2E7D32
