package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUISettingsUiState
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun NtfySection(
    state: ComfyUISettingsUiState,
    onTestNtfy: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            NtfySectionHeader(state)
            Spacer(modifier = Modifier.height(Spacing.sm))
            NtfySectionContent(state, onTestNtfy)
        }
    }
}

@Composable
private fun NtfySectionHeader(state: ComfyUISettingsUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.ntfy_section_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = if (state.isNtfySubscribed) {
                stringResource(R.string.ntfy_status_subscribed)
            } else {
                stringResource(R.string.ntfy_status_not_configured)
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (state.isNtfySubscribed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun NtfySectionContent(
    state: ComfyUISettingsUiState,
    onTestNtfy: () -> Unit,
) {
    val active = state.activeConnection
    if (active?.isNtfyConfigured == true) {
        Text(
            "${active.resolvedNtfyServerUrl}/${active.ntfyTopic}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        NtfyTestButton(state, onTestNtfy)
    } else {
        Text(
            stringResource(R.string.ntfy_setup_guide),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NtfyTestButton(
    state: ComfyUISettingsUiState,
    onTestNtfy: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        OutlinedButton(
            onClick = onTestNtfy,
            enabled = !state.isNtfyTestSending,
        ) {
            if (state.isNtfyTestSending) {
                CircularProgressIndicator(modifier = Modifier.size(Spacing.lg))
            } else {
                Text(stringResource(R.string.ntfy_test_notification))
            }
        }
        state.ntfyTestResult?.let { success ->
            Text(
                text = stringResource(
                    if (success) R.string.ntfy_test_success else R.string.ntfy_test_failed,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (success) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}
