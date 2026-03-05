package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.NavShortcut
import com.riox432.civitdeck.feature.settings.presentation.SettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavShortcutsSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = state.customNavShortcuts

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navigation Shortcuts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "Choose up to 2 shortcuts to pin to the bottom navigation bar.",
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NavShortcut.entries.forEach { shortcut ->
                val isSelected = shortcut in selected
                val isDisabled = !isSelected && selected.size >= 2
                NavShortcutRow(
                    shortcut = shortcut,
                    isSelected = isSelected,
                    isDisabled = isDisabled,
                    onToggle = {
                        val newList = if (isSelected) selected - shortcut else selected + shortcut
                        viewModel.onCustomNavShortcutsChanged(newList)
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun NavShortcutRow(
    shortcut: NavShortcut,
    isSelected: Boolean,
    isDisabled: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDisabled, onClick = onToggle, onClickLabel = "Toggle shortcut")
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            .alpha(if (isDisabled) 0.4f else 1f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(shortcut.label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = isSelected,
            onCheckedChange = { if (!isDisabled) onToggle() },
            enabled = !isDisabled,
        )
    }
}
