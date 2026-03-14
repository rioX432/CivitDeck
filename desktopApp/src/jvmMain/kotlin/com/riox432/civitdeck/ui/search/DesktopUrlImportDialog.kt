package com.riox432.civitdeck.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.riox432.civitdeck.ui.theme.Spacing

/**
 * Extracts model ID from a CivitAI URL.
 * Supports: https://civitai.com/models/12345 and https://civitai.com/models/12345/model-name
 */
internal fun extractModelId(url: String): Long? {
    val pattern = Regex("""civitai\.com/models/(\d+)""")
    return pattern.find(url)?.groupValues?.get(1)?.toLongOrNull()
}

@Composable
fun DesktopUrlImportDialog(
    onModelFound: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    val modelId = extractModelId(url)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && modelId != null) {
                onModelFound(modelId)
                true
            } else {
                false
            }
        },
        title = { Text("Import Model from URL") },
        text = {
            Column {
                Text(
                    "Paste a CivitAI model URL to open it directly.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("CivitAI URL") },
                    placeholder = { Text("https://civitai.com/models/...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = url.isNotBlank() && modelId == null,
                    supportingText = if (url.isNotBlank() && modelId == null) {
                        { Text("Could not parse model ID from URL") }
                    } else if (modelId != null) {
                        { Text("Model ID: $modelId") }
                    } else {
                        null
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { modelId?.let { onModelFound(it) } },
                enabled = modelId != null,
            ) {
                Text("Open")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
