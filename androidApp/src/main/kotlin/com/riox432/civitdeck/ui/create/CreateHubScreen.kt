package com.riox432.civitdeck.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
import com.riox432.civitdeck.domain.model.ExternalServerConnectionStatus
import com.riox432.civitdeck.domain.model.SDWebUIConnectionStatus
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsViewModel
import com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/** Navigation callbacks for the Create hub. */
data class CreateHubCallbacks(
    val onNavigateToComfyUI: () -> Unit = {},
    val onNavigateToComfyUIGeneration: () -> Unit = {},
    val onNavigateToComfyUIQueue: () -> Unit = {},
    val onNavigateToComfyUIHistory: () -> Unit = {},
    val onNavigateToOnboarding: () -> Unit = {},
    val onNavigateToSDWebUI: () -> Unit = {},
    val onNavigateToSDWebUIGeneration: () -> Unit = {},
    val onNavigateToExternalServer: () -> Unit = {},
    val onNavigateToExternalServerGallery: () -> Unit = {},
    val onNavigateToModelFiles: () -> Unit = {},
)

private enum class HubStatus { Connected, Error, Unknown }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHubScreen(callbacks: CreateHubCallbacks) {
    val comfyVm: ComfyUISettingsViewModel = koinViewModel()
    val sdVm: SDWebUISettingsViewModel = koinViewModel()
    val externalVm: ExternalServerSettingsViewModel = koinViewModel()
    val comfyState by comfyVm.uiState.collectAsStateWithLifecycle()
    val sdState by sdVm.uiState.collectAsStateWithLifecycle()
    val externalState by externalVm.uiState.collectAsStateWithLifecycle()

    val comfyConfigured = comfyState.activeConnection != null
    val sdConfigured = sdState.activeConnection != null
    val externalConfigured = externalState.activeConfig != null
    val nothingConfigured = !comfyConfigured && !sdConfigured && !externalConfigured

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.create_hub_title)) }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = Spacing.md,
                vertical = Spacing.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (nothingConfigured) {
                item { ConnectServerHero(onSetUp = callbacks.onNavigateToOnboarding) }
            }
            if (comfyConfigured) {
                item { ComfyUIServerCard(comfyState, callbacks) }
            }
            if (sdConfigured) {
                item { SDWebUIServerCard(sdState, callbacks) }
            }
            if (externalConfigured) {
                item { ExternalServerCard(externalState, callbacks) }
            }
            connectMoreItems(
                comfyConfigured = comfyConfigured,
                sdConfigured = sdConfigured,
                externalConfigured = externalConfigured,
                nothingConfigured = nothingConfigured,
                callbacks = callbacks,
            )
            item {
                UtilityRow(
                    icon = Icons.Default.Folder,
                    title = stringResource(R.string.create_hub_model_files),
                    subtitle = stringResource(R.string.create_hub_model_files_subtitle),
                    onClick = callbacks.onNavigateToModelFiles,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.connectMoreItems(
    comfyConfigured: Boolean,
    sdConfigured: Boolean,
    externalConfigured: Boolean,
    nothingConfigured: Boolean,
    callbacks: CreateHubCallbacks,
) {
    // The hero already offers ComfyUI setup when nothing is configured.
    val showComfy = !comfyConfigured && !nothingConfigured
    if (!showComfy && sdConfigured && externalConfigured) return
    item {
        Text(
            text = stringResource(R.string.create_hub_connect_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xxs),
        )
    }
    if (showComfy) {
        item {
            UtilityRow(Icons.Default.Palette, "ComfyUI", null, callbacks.onNavigateToComfyUI)
        }
    }
    if (!sdConfigured) {
        item {
            UtilityRow(Icons.Default.Memory, "SD WebUI", null, callbacks.onNavigateToSDWebUI)
        }
    }
    if (!externalConfigured) {
        item {
            UtilityRow(Icons.Default.Dns, "External Server", null, callbacks.onNavigateToExternalServer)
        }
    }
}

@Composable
private fun ComfyUIServerCard(
    state: com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsUiState,
    callbacks: CreateHubCallbacks,
) {
    ServerCard(
        icon = Icons.Default.Palette,
        title = "ComfyUI",
        subtitle = state.activeConnection?.let { "${it.hostname}:${it.port}" }.orEmpty(),
        status = state.connectionStatus.toHubStatus(),
        onClick = callbacks.onNavigateToComfyUI,
    ) {
        HubAction(Icons.Default.AutoAwesome, stringResource(R.string.create_hub_action_generate)) {
            callbacks.onNavigateToComfyUIGeneration()
        }
        HubAction(Icons.Default.Queue, stringResource(R.string.create_hub_action_queue)) {
            callbacks.onNavigateToComfyUIQueue()
        }
        HubAction(Icons.Default.PhotoLibrary, stringResource(R.string.create_hub_action_outputs)) {
            callbacks.onNavigateToComfyUIHistory()
        }
    }
}

@Composable
private fun SDWebUIServerCard(
    state: com.riox432.civitdeck.feature.comfyui.presentation.SDWebUISettingsUiState,
    callbacks: CreateHubCallbacks,
) {
    ServerCard(
        icon = Icons.Default.Memory,
        title = "SD WebUI",
        subtitle = state.activeConnection?.let { "${it.hostname}:${it.port}" }.orEmpty(),
        status = state.connectionStatus.toHubStatus(),
        onClick = callbacks.onNavigateToSDWebUI,
    ) {
        HubAction(Icons.Default.AutoAwesome, stringResource(R.string.create_hub_action_generate)) {
            callbacks.onNavigateToSDWebUIGeneration()
        }
    }
}

@Composable
private fun ExternalServerCard(
    state: com.riox432.civitdeck.feature.externalserver.presentation.ExternalServerSettingsUiState,
    callbacks: CreateHubCallbacks,
) {
    ServerCard(
        icon = Icons.Default.Dns,
        title = state.activeConfig?.name ?: "External Server",
        subtitle = state.activeConfig?.baseUrl.orEmpty(),
        status = state.connectionStatus.toHubStatus(),
        onClick = callbacks.onNavigateToExternalServer,
    ) {
        HubAction(Icons.Default.Image, stringResource(R.string.create_hub_action_gallery)) {
            callbacks.onNavigateToExternalServerGallery()
        }
    }
}

@Composable
private fun ConnectServerHero(onSetUp: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
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
                text = stringResource(R.string.create_hub_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.create_hub_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(onClick = onSetUp) {
                Text(stringResource(R.string.create_hub_empty_cta))
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
    actions: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.size(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StatusDot(status)
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) { actions() }
        }
    }
}

@Composable
private fun StatusDot(status: HubStatus) {
    val (color, label) = when (status) {
        HubStatus.Connected -> Color(GREEN_DOT) to stringResource(R.string.create_hub_status_online)
        HubStatus.Error -> MaterialTheme.colorScheme.error to stringResource(R.string.create_hub_status_offline)
        HubStatus.Unknown ->
            MaterialTheme.colorScheme.outline to stringResource(R.string.create_hub_status_unknown)
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
private fun HubAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.size(Spacing.xs))
        Text(label, style = MaterialTheme.typography.labelMedium)
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
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun ComfyUIConnectionStatus.toHubStatus(): HubStatus = when (this) {
    ComfyUIConnectionStatus.Connected -> HubStatus.Connected
    ComfyUIConnectionStatus.Error -> HubStatus.Error
    // Disconnected means "saved but not tested yet", not a failed test.
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
