package com.riox432.civitdeck.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.model.NavShortcut
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.ThemeMode
import com.riox432.civitdeck.domain.model.TimePeriod
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DisplaySettingsUiState(
    val defaultSortOrder: SortOrder = SortOrder.MostDownloaded,
    val defaultTimePeriod: TimePeriod = TimePeriod.AllTime,
    val gridColumns: Int = 2,
    val accentColor: AccentColor = AccentColor.Blue,
    val amoledDarkMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val customNavShortcuts: List<NavShortcut> = emptyList(),
)

class DisplaySettingsViewModel(
    private val displayUseCases: DisplayPreferenceUseCases,
    private val appearanceUseCases: AppearanceUseCases,
) : ViewModel() {

    val uiState: StateFlow<DisplaySettingsUiState> = combine(
        displayUseCases.observeDefaultSortOrder(),
        displayUseCases.observeDefaultTimePeriod(),
        displayUseCases.observeGridColumns(),
        appearanceUseCases.observeAccentColor(),
        appearanceUseCases.observeAmoledDarkMode(),
    ) { sort, period, columns, accent, amoled ->
        DisplaySettingsUiState(
            defaultSortOrder = sort,
            defaultTimePeriod = period,
            gridColumns = columns,
            accentColor = accent,
            amoledDarkMode = amoled,
        )
    }.combine(appearanceUseCases.observeThemeMode()) { state, mode ->
        state.copy(themeMode = mode)
    }.combine(appearanceUseCases.observeCustomNavShortcuts()) { state, shortcuts ->
        state.copy(customNavShortcuts = shortcuts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplaySettingsUiState())

    fun onSortOrderChanged(sort: SortOrder) {
        viewModelScope.launch { displayUseCases.setDefaultSortOrder(sort) }
    }

    fun onTimePeriodChanged(period: TimePeriod) {
        viewModelScope.launch { displayUseCases.setDefaultTimePeriod(period) }
    }

    fun onGridColumnsChanged(columns: Int) {
        viewModelScope.launch { displayUseCases.setGridColumns(columns) }
    }

    fun onAccentColorChanged(color: AccentColor) {
        viewModelScope.launch { appearanceUseCases.setAccentColor(color) }
    }

    fun onAmoledDarkModeChanged(enabled: Boolean) {
        viewModelScope.launch { appearanceUseCases.setAmoledDarkMode(enabled) }
    }

    fun onThemeModeChanged(mode: ThemeMode) {
        viewModelScope.launch { appearanceUseCases.setThemeMode(mode) }
    }

    fun onCustomNavShortcutsChanged(shortcuts: List<NavShortcut>) {
        viewModelScope.launch { appearanceUseCases.setCustomNavShortcuts(shortcuts) }
    }
}
