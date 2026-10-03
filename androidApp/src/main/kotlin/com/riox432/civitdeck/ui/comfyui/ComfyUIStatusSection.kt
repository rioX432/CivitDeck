package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
import com.riox432.civitdeck.domain.model.ConnectionSecurityLevel
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsUiState
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun StatusSection(
    state: ComfyUISettingsUiState,
    onTest: () -> Unit,
    onReviewCertificate: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state.connectionStatus) {
                ComfyUIConnectionStatus.Connected ->
                    MaterialTheme.colorScheme.primaryContainer
                ComfyUIConnectionStatus.Error ->
                    MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            StatusHeader(state, onTest, onReviewCertificate)
            state.testError?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusHeader(
    state: ComfyUISettingsUiState,
    onTest: () -> Unit,
    onReviewCertificate: (Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Weighted so long text wraps instead of pushing the action buttons out of the card. The
        // badge flows to its own line rather than breaking mid-word when two buttons leave little room.
        Column(modifier = Modifier.weight(1f)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(statusLabel(state.connectionStatus), style = MaterialTheme.typography.titleMedium)
                state.securityLevel?.let { level ->
                    SecurityBadge(level, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
            state.activeConnection?.let {
                Text(it.baseUrl, style = MaterialTheme.typography.bodySmall)
            }
        }
        val active = state.activeConnection
        if (state.isTesting) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else if (active != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pinned trust applies to every HTTPS self-signed row, including those with no pin
                // stored yet, so the review is offered whether or not a fingerprint is saved.
                if (active.useHttps && active.acceptSelfSigned) {
                    TextButton(onClick = { onReviewCertificate(active.id) }) {
                        Text(stringResource(R.string.comfyui_review_certificate))
                    }
                }
                TextButton(onClick = onTest) { Text(stringResource(R.string.action_test)) }
            }
        }
    }
}

@Composable
private fun SecurityBadge(level: ConnectionSecurityLevel, modifier: Modifier = Modifier) {
    val (icon, color, label) = when (level) {
        ConnectionSecurityLevel.Secure -> Triple("lock", MaterialTheme.colorScheme.primary, "HTTPS")
        ConnectionSecurityLevel.SelfSigned -> Triple("lock", MaterialTheme.colorScheme.tertiary, "Self-signed")
        ConnectionSecurityLevel.LocalInsecure -> Triple("wifi", MaterialTheme.colorScheme.onSurfaceVariant, "LAN")
        ConnectionSecurityLevel.RemoteInsecure -> Triple("warning", MaterialTheme.colorScheme.error, "HTTP")
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

@Composable
private fun statusLabel(status: ComfyUIConnectionStatus): String = when (status) {
    ComfyUIConnectionStatus.Connected -> stringResource(R.string.comfyui_status_connected)
    ComfyUIConnectionStatus.Disconnected -> stringResource(R.string.comfyui_status_disconnected)
    ComfyUIConnectionStatus.Testing -> stringResource(R.string.comfyui_status_testing)
    ComfyUIConnectionStatus.Error -> stringResource(R.string.comfyui_status_error)
    ComfyUIConnectionStatus.NotConfigured -> stringResource(R.string.comfyui_status_not_configured)
}
