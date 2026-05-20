package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.UpdateResult
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.update.DesktopUpdateViewModel
import java.net.URI
import java.awt.Desktop as AwtDesktop

@Composable
internal fun UpdateSection(viewModel: DesktopUpdateViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "Updates") {
        if (state.showBanner) {
            state.updateResult?.let { result ->
                UpdateBanner(
                    result = result,
                    onDismiss = viewModel::dismissBanner,
                )
            }
        }
        SwitchSetting(
            label = "Auto-check for updates",
            checked = state.autoCheckEnabled,
            onCheckedChange = viewModel::setAutoCheckEnabled,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        OutlinedButton(
            onClick = viewModel::checkForUpdate,
            enabled = !state.isChecking,
        ) {
            Text(if (state.isChecking) "Checking..." else "Check now")
        }
    }
}

@Composable
private fun UpdateBanner(
    result: UpdateResult,
    onDismiss: () -> Unit,
) {
    Text(
        text = "Update available: v${result.currentVersion} → v${result.latestVersion}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Button(onClick = {
            if (AwtDesktop.isDesktopSupported()) {
                AwtDesktop.getDesktop().browse(URI(result.htmlUrl))
            }
        }) {
            Text("Download")
        }
        OutlinedButton(onClick = onDismiss) {
            Text("Dismiss")
        }
    }
    Spacer(modifier = Modifier.height(Spacing.md))
}
