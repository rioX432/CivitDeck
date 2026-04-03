package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.riox432.civitdeck.domain.model.ExternalServerConnectionStatus
import com.riox432.civitdeck.ui.externalserver.ExternalServerGalleryViewModel
import com.riox432.civitdeck.ui.externalserver.ExternalServerSettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun ExternalServerSettingsSection(viewModel: ExternalServerSettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var nameInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf("") }

    SettingsCard(title = "External Server") {
        ConnectionStatusBadge(
            label = "External Server",
            status = state.connectionStatus.name,
            isConnected = state.connectionStatus == ExternalServerConnectionStatus.Connected,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))

        state.activeConfig?.let { active ->
            Text(
                text = "Active: ${active.name} (${active.baseUrl})",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            OutlinedButton(
                onClick = viewModel::onTestConnection,
                enabled = !state.isTesting,
            ) {
                Text(if (state.isTesting) "Testing..." else "Test Connection")
            }
            state.testError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (state.configs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text("Saved Configs:", style = MaterialTheme.typography.labelMedium)
            state.configs.forEach { config ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${config.name} - ${config.baseUrl}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Row {
                        TextButton(onClick = { viewModel.onActivateConfig(config.id) }) {
                            Text("Activate")
                        }
                        TextButton(onClick = { viewModel.onDeleteConfig(config.id) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        Text("Add Server:", style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("Base URL") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            label = { Text("API Key (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Button(
            onClick = {
                viewModel.onSaveConfig(nameInput, urlInput, apiKeyInput)
                nameInput = ""
                urlInput = ""
                apiKeyInput = ""
            },
            enabled = nameInput.isNotBlank() && urlInput.isNotBlank(),
        ) {
            Text("Save Config")
        }
    }
}

@Composable
fun ExternalServerGallerySection(viewModel: ExternalServerGalleryViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "External Server Gallery") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${state.images.size} image(s) loaded (page ${state.currentPage}/${state.totalPages})",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(
                    onClick = viewModel::onRefresh,
                    enabled = !state.isRefreshing && !state.isLoading,
                ) {
                    Text("Refresh")
                }
                if (state.currentPage < state.totalPages) {
                    OutlinedButton(
                        onClick = viewModel::onLoadMore,
                        enabled = !state.isLoadingMore,
                    ) {
                        Text("Load More")
                    }
                }
            }
        }
        state.error?.let { error ->
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (state.isLoading) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text("Loading...", style = MaterialTheme.typography.bodySmall)
        }
    }
}
