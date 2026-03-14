@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.plugin

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.InstalledPlugin
import com.riox432.civitdeck.domain.model.InstalledPluginState
import com.riox432.civitdeck.domain.model.InstalledPluginType
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginManagementScreen(
    viewModel: PluginManagementViewModel,
    onBack: () -> Unit,
    onPluginClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plugins") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { padding ->
        PluginListContent(
            state = state,
            viewModel = viewModel,
            onPluginClick = onPluginClick,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
private fun PluginListContent(
    state: PluginManagementUiState,
    viewModel: PluginManagementViewModel,
    onPluginClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> LoadingContent(modifier)
        state.plugins.isEmpty() -> EmptyContent(modifier)
        else -> PluginList(state.plugins, viewModel, onPluginClick, modifier)
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    EmptyStateMessage(
        icon = Icons.Default.Extension,
        title = "No plugins installed",
        subtitle = "Plugins extend the app with new features",
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun PluginList(
    plugins: List<InstalledPlugin>,
    viewModel: PluginManagementViewModel,
    onPluginClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        items(plugins, key = { it.id }) { plugin ->
            PluginRow(
                plugin = plugin,
                isActive = viewModel.isPluginActive(plugin),
                onToggle = { active -> viewModel.togglePlugin(plugin.id, active) },
                onClick = { onPluginClick(plugin.id) },
            )
        }
    }
}

@Composable
private fun PluginRow(
    plugin: InstalledPlugin,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "View plugin details", onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        PluginIcon()
        PluginInfo(plugin, Modifier.weight(1f))
        StatusIndicator(plugin.state)
        Switch(checked = isActive, onCheckedChange = onToggle)
    }
}

@Composable
private fun PluginIcon() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(Spacing.sm))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Extension,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PluginInfo(plugin: InstalledPlugin, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(plugin.name, style = MaterialTheme.typography.bodyLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "v${plugin.version}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PluginTypeBadge(plugin.pluginType)
        }
    }
}

@Composable
internal fun PluginTypeBadge(type: InstalledPluginType) {
    val label = when (type) {
        InstalledPluginType.WORKFLOW_ENGINE -> "Workflow"
        InstalledPluginType.EXPORT_FORMAT -> "Export"
        InstalledPluginType.THEME -> "Theme"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(Spacing.xs),
            )
            .padding(horizontal = Spacing.sm, vertical = 2.dp),
    )
}

@Composable
private fun StatusIndicator(state: InstalledPluginState) {
    val color = when (state) {
        InstalledPluginState.ACTIVE -> MaterialTheme.colorScheme.primary
        InstalledPluginState.INSTALLED -> MaterialTheme.colorScheme.outline
        InstalledPluginState.INACTIVE -> MaterialTheme.colorScheme.outline
        InstalledPluginState.ERROR -> MaterialTheme.colorScheme.error
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color),
    )
}
