package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun AccountSection(
    apiKey: String?,
    connectedUsername: String?,
    isValidating: Boolean,
    error: String?,
    onValidateAndSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (apiKey != null && connectedUsername != null) {
        ConnectedAccountRow(connectedUsername, onClear)
    } else {
        ApiKeyInputRow(isValidating, error, onValidateAndSave)
    }
}

@Composable
internal fun ConnectedAccountRow(username: String, onClear: () -> Unit) {
    var showConfirmation by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_connected_as),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(username, style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = { showConfirmation = true }) {
            Text(stringResource(R.string.settings_disconnect), color = MaterialTheme.colorScheme.error)
        }
    }
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text(stringResource(R.string.settings_disconnect)) },
            text = { Text(stringResource(R.string.settings_remove_api_key)) },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    showConfirmation = false
                }) { Text(stringResource(R.string.settings_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) { Text(stringResource(R.string.settings_cancel)) }
            },
        )
    }
}

@Composable
internal fun ApiKeyInputRow(
    isValidating: Boolean,
    error: String?,
    onValidateAndSave: (String) -> Unit,
) {
    var keyInput by rememberSaveable { mutableStateOf("") }
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                placeholder = { Text(stringResource(R.string.settings_paste_api_key)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = error != null,
                modifier = Modifier.weight(1f),
            )
            if (isValidating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                TextButton(
                    onClick = {
                        onValidateAndSave(keyInput)
                        keyInput = ""
                    },
                    enabled = keyInput.isNotBlank(),
                ) { Text(stringResource(R.string.settings_verify)) }
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Text(
            stringResource(R.string.settings_api_key_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}
