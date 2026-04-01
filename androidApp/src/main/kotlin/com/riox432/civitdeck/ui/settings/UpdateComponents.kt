package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.R
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun UpdateCheckRow(
    autoCheckEnabled: Boolean,
    isChecking: Boolean,
    onAutoCheckChanged: (Boolean) -> Unit,
    onCheckNow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.settings_auto_check_updates), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_auto_check_updates_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = autoCheckEnabled, onCheckedChange = onAutoCheckChanged)
        }
        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TextButton(onClick = onCheckNow, enabled = !isChecking) {
                Text(
                    if (isChecking) {
                        stringResource(
                            R.string.settings_checking
                        )
                    } else {
                        stringResource(R.string.settings_check_now)
                    }
                )
            }
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }
    }
}
