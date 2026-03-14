package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.CivitaiLinkActivity
import com.riox432.civitdeck.domain.model.CivitaiLinkStatus
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSettingsUiState
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSettingsViewModel
import com.riox432.civitdeck.ui.theme.CivitDeckColors
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivitaiLinkSettingsScreen(
    viewModel: CivitaiLinkSettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Civitai Link") },
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
        LazyColumn(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item { SubscriptionRequiredBanner() }
            item { StatusCard(state = state, onDisconnect = viewModel::onDisconnect) }
            item {
                ConfigCard(
                    state = state,
                    onKeyChanged = viewModel::onKeyChanged,
                    onSaveAndConnect = viewModel::onSaveAndConnect,
                )
            }
            if (state.activities.isNotEmpty()) {
                item { ActivityListHeader() }
                items(state.activities, key = { it.id }) { activity ->
                    ActivityCard(
                        activity = activity,
                        onCancel = { viewModel.onCancelActivity(activity.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    state: CivitaiLinkSettingsUiState,
    onDisconnect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(status = state.status)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = state.status.label(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.status == CivitaiLinkStatus.Connected) {
                TextButton(onClick = onDisconnect) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun StatusDot(status: CivitaiLinkStatus) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(color = status.dotColor(), shape = CircleShape),
    )
}

@Composable
private fun ConfigCard(
    state: CivitaiLinkSettingsUiState,
    onKeyChanged: (String) -> Unit,
    onSaveAndConnect: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text("Configuration", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = state.linkKey,
                onValueChange = onKeyChanged,
                label = { Text("Link Key") },
                placeholder = { Text("Paste your Civitai Link key here") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Get your link key from civitai.com \u2192 Account Settings \u2192 Civitai Link",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onSaveAndConnect,
                enabled = state.linkKey.isNotBlank() && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Connecting..." else "Save & Connect")
            }
        }
    }
}

@Composable
private fun SubscriptionRequiredBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                "Requires CivitAI Supporter+ subscription",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                "Civitai Link is only available to CivitAI Supporter+ members. Subscribe at civitai.com/pricing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun ActivityListHeader() {
    Text(
        text = "Downloads on PC",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
    )
}

@Composable
private fun ActivityCard(
    activity: CivitaiLinkActivity,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(activity.type, style = MaterialTheme.typography.bodyMedium)
                Text(
                    activity.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (activity.status == "Running") {
                LinearProgressIndicator(
                    progress = { activity.progress.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

private fun CivitaiLinkStatus.label(): String = when (this) {
    CivitaiLinkStatus.Connected -> "Connected"
    CivitaiLinkStatus.Connecting -> "Connecting..."
    CivitaiLinkStatus.Error -> "Connection Error"
    CivitaiLinkStatus.Disconnected -> "Disconnected"
}

private fun CivitaiLinkStatus.dotColor(): Color = when (this) {
    CivitaiLinkStatus.Connected -> CivitDeckColors.statusSuccess
    CivitaiLinkStatus.Connecting -> CivitDeckColors.statusWarning
    CivitaiLinkStatus.Error -> CivitDeckColors.statusError
    CivitaiLinkStatus.Disconnected -> CivitDeckColors.statusNeutral
}
