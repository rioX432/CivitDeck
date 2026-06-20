package com.riox432.civitdeck.ui.settings

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun AuthSettingsSection(viewModel: AuthSettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var apiKeyInput by remember { mutableStateOf("") }

    SettingsCard(title = "Authentication") {
        val connectedUsername = state.connectedUsername
        if (connectedUsername != null) {
            ConnectedState(
                username = connectedUsername,
                onRefresh = viewModel::onRefreshUsername,
                onDisconnect = viewModel::onClearApiKey,
            )
        } else {
            DisconnectedState(
                apiKeyInput = apiKeyInput,
                onApiKeyInputChanged = { apiKeyInput = it },
                apiKeyError = state.apiKeyError,
                isValidating = state.isValidatingApiKey,
                onValidate = { viewModel.onValidateAndSaveApiKey(apiKeyInput) },
            )
        }
    }
}

@Composable
private fun ConnectedState(
    username: String,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Text(
        text = "Connected as: $username",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    Row {
        OutlinedButton(onClick = onRefresh) { Text("Refresh") }
        Spacer(modifier = Modifier.width(Spacing.sm))
        OutlinedButton(onClick = onDisconnect) { Text("Disconnect") }
    }
}

@Composable
private fun DisconnectedState(
    apiKeyInput: String,
    onApiKeyInputChanged: (String) -> Unit,
    apiKeyError: String?,
    isValidating: Boolean,
    onValidate: () -> Unit,
) {
    OutlinedTextField(
        value = apiKeyInput,
        onValueChange = onApiKeyInputChanged,
        label = { Text("CivitAI API Key") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        isError = apiKeyError != null,
        supportingText = apiKeyError?.let { { Text(it) } },
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    Button(
        onClick = onValidate,
        enabled = !isValidating && apiKeyInput.isNotBlank(),
    ) {
        Text(if (isValidating) "Validating..." else "Connect")
    }
}
