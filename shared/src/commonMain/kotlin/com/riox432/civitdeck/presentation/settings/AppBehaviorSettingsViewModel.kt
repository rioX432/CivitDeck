package com.riox432.civitdeck.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.usecase.ObserveNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObservePollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveQualityThresholdUseCase
import com.riox432.civitdeck.domain.usecase.SetNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetPollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.SetPowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.SetQualityThresholdUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppBehaviorSettingsUiState(
    val powerUserMode: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val pollingInterval: PollingInterval = PollingInterval.Off,
    val feedQualityThreshold: Int = 30,
)

@Suppress("LongParameterList")
class AppBehaviorSettingsViewModel(
    observePowerUserModeUseCase: ObservePowerUserModeUseCase,
    private val setPowerUserModeUseCase: SetPowerUserModeUseCase,
    observeNotificationsEnabledUseCase: ObserveNotificationsEnabledUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    observePollingIntervalUseCase: ObservePollingIntervalUseCase,
    private val setPollingIntervalUseCase: SetPollingIntervalUseCase,
    observeQualityThresholdUseCase: ObserveQualityThresholdUseCase,
    private val setQualityThresholdUseCase: SetQualityThresholdUseCase,
) : ViewModel() {

    val uiState: StateFlow<AppBehaviorSettingsUiState> = combine(
        observePowerUserModeUseCase(),
        observeNotificationsEnabledUseCase(),
        observePollingIntervalUseCase(),
        observeQualityThresholdUseCase(),
    ) { powerUser, notifications, polling, qualityThreshold ->
        AppBehaviorSettingsUiState(
            powerUserMode = powerUser,
            notificationsEnabled = notifications,
            pollingInterval = polling,
            feedQualityThreshold = qualityThreshold,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppBehaviorSettingsUiState())

    fun onPowerUserModeChanged(enabled: Boolean) {
        viewModelScope.launch { setPowerUserModeUseCase(enabled) }
    }

    fun onNotificationsEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            setNotificationsEnabledUseCase(enabled)
            if (enabled && uiState.value.pollingInterval == PollingInterval.Off) {
                setPollingIntervalUseCase(PollingInterval.FifteenMinutes)
            }
        }
    }

    fun onPollingIntervalChanged(interval: PollingInterval) {
        viewModelScope.launch { setPollingIntervalUseCase(interval) }
    }

    fun onFeedQualityThresholdChanged(threshold: Int) {
        viewModelScope.launch { setQualityThresholdUseCase(threshold) }
    }
}
