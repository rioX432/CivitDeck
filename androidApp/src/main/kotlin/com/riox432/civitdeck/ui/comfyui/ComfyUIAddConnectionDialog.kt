package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.ui.theme.Spacing

private const val TOPIC_ID_LENGTH = 8

@Composable
internal fun AddConnectionDialog(
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
            AddConnectionConfirmButton(
                name = name,
                hostname = hostname,
                portText = portText,
                useHttps = useHttps,
                acceptSelfSigned = acceptSelfSigned,
                ntfyServerUrl = ntfyServerUrl,
                ntfyTopic = ntfyTopic,
                onSave = onSave,
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
@Suppress("LongParameterList")
private fun AddConnectionConfirmButton(
    name: String,
    hostname: String,
    portText: String,
    useHttps: Boolean,
    acceptSelfSigned: Boolean,
    ntfyServerUrl: String,
    ntfyTopic: String,
    onSave: (
        name: String,
        hostname: String,
        port: Int,
        useHttps: Boolean,
        acceptSelfSigned: Boolean,
        ntfyServerUrl: String?,
        ntfyTopic: String?,
    ) -> Unit,
) {
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
}

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
