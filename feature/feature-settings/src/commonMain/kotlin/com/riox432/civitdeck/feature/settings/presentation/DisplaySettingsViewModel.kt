package com.riox432.civitdeck.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.model.NavShortcut
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.ThemeMode
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCustomNavShortcutsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveThemeModeUseCase
import com.riox432.civitdeck.domain.usecase.SetAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.SetAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.SetCustomNavShortcutsUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.SetGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.SetThemeModeUseCase
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

@Suppress("LongParameterList")
class DisplaySettingsViewModel(
    observeDefaultSortOrderUseCase: ObserveDefaultSortOrderUseCase,
    private val setDefaultSortOrderUseCase: SetDefaultSortOrderUseCase,
    observeDefaultTimePeriodUseCase: ObserveDefaultTimePeriodUseCase,
    private val setDefaultTimePeriodUseCase: SetDefaultTimePeriodUseCase,
    observeGridColumnsUseCase: ObserveGridColumnsUseCase,
    private val setGridColumnsUseCase: SetGridColumnsUseCase,
    observeAccentColorUseCase: ObserveAccentColorUseCase,
    private val setAccentColorUseCase: SetAccentColorUseCase,
    observeAmoledDarkModeUseCase: ObserveAmoledDarkModeUseCase,
    private val setAmoledDarkModeUseCase: SetAmoledDarkModeUseCase,
    observeThemeModeUseCase: ObserveThemeModeUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    observeCustomNavShortcutsUseCase: ObserveCustomNavShortcutsUseCase,
    private val setCustomNavShortcutsUseCase: SetCustomNavShortcutsUseCase,
) : ViewModel() {

    val uiState: StateFlow<DisplaySettingsUiState> = combine(
        observeDefaultSortOrderUseCase(),
        observeDefaultTimePeriodUseCase(),
        observeGridColumnsUseCase(),
        observeAccentColorUseCase(),
        observeAmoledDarkModeUseCase(),
    ) { sort, period, columns, accent, amoled ->
        DisplaySettingsUiState(
            defaultSortOrder = sort,
            defaultTimePeriod = period,
            gridColumns = columns,
            accentColor = accent,
            amoledDarkMode = amoled,
        )
    }.combine(observeThemeModeUseCase()) { state, mode ->
        state.copy(themeMode = mode)
    }.combine(observeCustomNavShortcutsUseCase()) { state, shortcuts ->
        state.copy(customNavShortcuts = shortcuts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplaySettingsUiState())

    fun onSortOrderChanged(sort: SortOrder) {
        viewModelScope.launch { setDefaultSortOrderUseCase(sort) }
    }

    fun onTimePeriodChanged(period: TimePeriod) {
        viewModelScope.launch { setDefaultTimePeriodUseCase(period) }
    }

    fun onGridColumnsChanged(columns: Int) {
        viewModelScope.launch { setGridColumnsUseCase(columns) }
    }

    fun onAccentColorChanged(color: AccentColor) {
        viewModelScope.launch { setAccentColorUseCase(color) }
    }

    fun onAmoledDarkModeChanged(enabled: Boolean) {
        viewModelScope.launch { setAmoledDarkModeUseCase(enabled) }
    }

    fun onThemeModeChanged(mode: ThemeMode) {
        viewModelScope.launch { setThemeModeUseCase(mode) }
    }

    fun onCustomNavShortcutsChanged(shortcuts: List<NavShortcut>) {
        viewModelScope.launch { setCustomNavShortcutsUseCase(shortcuts) }
    }
}
