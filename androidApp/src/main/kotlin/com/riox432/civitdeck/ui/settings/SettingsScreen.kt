package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
) {
    val nsfwFilterLevel by viewModel.nsfwFilterLevel.collectAsStateWithLifecycle()

    Column {
        NsfwFilterRow(
            nsfwFilterLevel = nsfwFilterLevel,
            onToggle = {
                val newLevel = if (nsfwFilterLevel == NsfwFilterLevel.Off) {
                    NsfwFilterLevel.All
                } else {
                    NsfwFilterLevel.Off
                }
                viewModel.onNsfwFilterChanged(newLevel)
            },
        )
    }
}

@Composable
private fun NsfwFilterRow(
    nsfwFilterLevel: NsfwFilterLevel,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "NSFW Content",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Show NSFW content in search results",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = nsfwFilterLevel != NsfwFilterLevel.Off,
            onCheckedChange = { onToggle() },
        )
    }
}
