package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.model.ThemeMode
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun DisplaySettingsSection(viewModel: DisplaySettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "Display") {
        SettingsDropdown(
            label = "Theme",
            selected = state.themeMode.name,
            options = ThemeMode.entries.map { it.name },
            onSelected = { viewModel.onThemeModeChanged(ThemeMode.valueOf(it)) },
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        SettingsDropdown(
            label = "Accent Color",
            selected = state.accentColor.displayName,
            options = AccentColor.entries.map { it.displayName },
            onSelected = { name ->
                AccentColor.entries.find { it.displayName == name }
                    ?.let { viewModel.onAccentColorChanged(it) }
            },
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        SliderSetting(
            label = "Grid Columns",
            value = state.gridColumns.toFloat(),
            valueRange = 1f..6f,
            steps = 4,
            valueLabel = state.gridColumns.toString(),
            onValueChange = { viewModel.onGridColumnsChanged(it.toInt()) },
        )
    }
}
