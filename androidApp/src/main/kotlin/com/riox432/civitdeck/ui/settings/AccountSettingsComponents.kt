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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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
                "Connected as",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(username, style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = { showConfirmation = true }) {
            Text("Disconnect", color = MaterialTheme.colorScheme.error)
        }
    }
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Disconnect") },
            text = { Text("Remove your CivitAI API key?") },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    showConfirmation = false
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) { Text("Cancel") }
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
                placeholder = { Text("Paste API key") },
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
                ) { Text("Verify") }
            }
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Text(
            "Get your key at civitai.com/user/account",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}
