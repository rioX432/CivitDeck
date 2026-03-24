package com.riox432.civitdeck.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun NotificationsToggleRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) onToggle(true) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Model Update Alerts", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Notify when favorited models get new versions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { newValue ->
                if (newValue && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onToggle(newValue)
                }
            },
        )
    }
}

@Composable
internal fun PollingIntervalRow(selected: PollingInterval, onChanged: (PollingInterval) -> Unit) {
    val options = PollingInterval.entries.filter { it != PollingInterval.Off }
    DropdownSettingRow(
        label = "Check Frequency",
        value = selected.displayName,
        options = options.map { it.displayName },
        onSelected = { name -> options.find { it.displayName == name }?.let(onChanged) },
    )
}
