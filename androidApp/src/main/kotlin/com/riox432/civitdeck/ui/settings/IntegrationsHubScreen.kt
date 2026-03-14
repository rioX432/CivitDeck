package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.usecase.ObserveCivitaiLinkKeyUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveActiveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveActiveSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ObserveActiveExternalServerConfigUseCase
import com.riox432.civitdeck.ui.theme.Spacing
import org.koin.compose.koinInject

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsHubScreen(
    onBack: () -> Unit,
    onNavigateToComfyUI: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToSDWebUI: () -> Unit,
    onNavigateToCivitaiLink: () -> Unit,
    onNavigateToExternalServer: () -> Unit,
) {
    val observeComfyUI = koinInject<ObserveActiveComfyUIConnectionUseCase>()
    val observeSDWebUI = koinInject<ObserveActiveSDWebUIConnectionUseCase>()
    val observeCivitaiLink = koinInject<ObserveCivitaiLinkKeyUseCase>()
    val observeExtServer = koinInject<ObserveActiveExternalServerConfigUseCase>()

    val comfyUIName = observeComfyUI().collectAsStateWithLifecycle(null).value?.name
    val sdWebUIName = observeSDWebUI().collectAsStateWithLifecycle(null).value?.name
    val civitaiLinkKey by observeCivitaiLink().collectAsStateWithLifecycle(null)
    val extServerName = observeExtServer().collectAsStateWithLifecycle(null).value?.name

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Integrations") },
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
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            integrationItems(
                comfyUIName = comfyUIName,
                sdWebUIName = sdWebUIName,
                civitaiLinkConnected = civitaiLinkKey != null,
                extServerName = extServerName,
                onNavigateToComfyUI = onNavigateToComfyUI,
                onNavigateToTemplates = onNavigateToTemplates,
                onNavigateToSDWebUI = onNavigateToSDWebUI,
                onNavigateToCivitaiLink = onNavigateToCivitaiLink,
                onNavigateToExternalServer = onNavigateToExternalServer,
            )
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.integrationItems(
    comfyUIName: String?,
    sdWebUIName: String?,
    civitaiLinkConnected: Boolean,
    extServerName: String?,
    onNavigateToComfyUI: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToSDWebUI: () -> Unit,
    onNavigateToCivitaiLink: () -> Unit,
    onNavigateToExternalServer: () -> Unit,
) {
    item { SectionHeader("ComfyUI") }
    item { IntegrationRow("Server Connections", comfyUIName ?: "Not configured", onNavigateToComfyUI) }
    item { IntegrationRow("Workflow Templates", onClick = onNavigateToTemplates) }
    item { SectionHeader("SD WebUI") }
    item { IntegrationRow("Connections", sdWebUIName ?: "Not configured", onNavigateToSDWebUI) }
    item { SectionHeader("Civitai Link") }
    item {
        IntegrationRow(
            "Setup",
            if (civitaiLinkConnected) "Connected" else "Not configured",
            onNavigateToCivitaiLink,
        )
    }
    item { SectionHeader("Custom Server") }
    item { IntegrationRow("Server Configuration", extServerName ?: "Not configured", onNavigateToExternalServer) }
}

@Composable
private fun IntegrationRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open integration", onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
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
            contentDescription = "Navigate forward",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
