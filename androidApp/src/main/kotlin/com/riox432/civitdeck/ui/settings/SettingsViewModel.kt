package com.riox432.civitdeck.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.model.CacheInfo
import com.riox432.civitdeck.domain.model.HiddenModel
import com.riox432.civitdeck.domain.model.NsfwBlurSettings
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.usecase.AddExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.EvictCacheUseCase
import com.riox432.civitdeck.domain.usecase.GetCacheInfoUseCase
import com.riox432.civitdeck.domain.usecase.GetExcludedTagsUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.ObserveDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.ObserveGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNetworkStatusUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.ObservePollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.RemoveExcludedTagUseCase
import com.riox432.civitdeck.domain.usecase.SetAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.SetAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.SetApiKeyUseCase
import com.riox432.civitdeck.domain.usecase.SetCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultSortOrderUseCase
import com.riox432.civitdeck.domain.usecase.SetDefaultTimePeriodUseCase
import com.riox432.civitdeck.domain.usecase.SetGridColumnsUseCase
import com.riox432.civitdeck.domain.usecase.SetNotificationsEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwBlurSettingsUseCase
import com.riox432.civitdeck.domain.usecase.SetNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.SetOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetPollingIntervalUseCase
import com.riox432.civitdeck.domain.usecase.SetPowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase
import com.riox432.civitdeck.domain.usecase.ValidateApiKeyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val nsfwFilterLevel: NsfwFilterLevel = NsfwFilterLevel.Off,
    val nsfwBlurSettings: NsfwBlurSettings = NsfwBlurSettings(),
    val defaultSortOrder: SortOrder = SortOrder.MostDownloaded,
    val defaultTimePeriod: TimePeriod = TimePeriod.AllTime,
    val gridColumns: Int = 2,
    val hiddenModels: List<HiddenModel> = emptyList(),
    val excludedTags: List<String> = emptyList(),
    val apiKey: String? = null,
    val connectedUsername: String? = null,
    val isValidatingApiKey: Boolean = false,
    val apiKeyError: String? = null,
    val powerUserMode: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val pollingInterval: PollingInterval = PollingInterval.Off,
    val accentColor: AccentColor = AccentColor.Blue,
    val amoledDarkMode: Boolean = false,
    val isOnline: Boolean = true,
    val offlineCacheEnabled: Boolean = true,
    val cacheSizeLimitMb: Int = 200,
    val cacheInfo: CacheInfo = CacheInfo(0, 0),
)

@Suppress("LongParameterList")
class SettingsViewModel(
    observeNsfwFilterUseCase: ObserveNsfwFilterUseCase,
    private val setNsfwFilterUseCase: SetNsfwFilterUseCase,
    observeNsfwBlurSettingsUseCase: ObserveNsfwBlurSettingsUseCase,
    private val setNsfwBlurSettingsUseCase: SetNsfwBlurSettingsUseCase,
    observeDefaultSortOrderUseCase: ObserveDefaultSortOrderUseCase,
    private val setDefaultSortOrderUseCase: SetDefaultSortOrderUseCase,
    observeDefaultTimePeriodUseCase: ObserveDefaultTimePeriodUseCase,
    private val setDefaultTimePeriodUseCase: SetDefaultTimePeriodUseCase,
    observeGridColumnsUseCase: ObserveGridColumnsUseCase,
    private val setGridColumnsUseCase: SetGridColumnsUseCase,
    private val getHiddenModelsUseCase: GetHiddenModelsUseCase,
    private val unhideModelUseCase: UnhideModelUseCase,
    private val getExcludedTagsUseCase: GetExcludedTagsUseCase,
    private val addExcludedTagUseCase: AddExcludedTagUseCase,
    private val removeExcludedTagUseCase: RemoveExcludedTagUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    private val clearBrowsingHistoryUseCase: ClearBrowsingHistoryUseCase,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val observeApiKeyUseCase: ObserveApiKeyUseCase,
    private val setApiKeyUseCase: SetApiKeyUseCase,
    private val validateApiKeyUseCase: ValidateApiKeyUseCase,
    observePowerUserModeUseCase: ObservePowerUserModeUseCase,
    private val setPowerUserModeUseCase: SetPowerUserModeUseCase,
    observeNotificationsEnabledUseCase: ObserveNotificationsEnabledUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    observePollingIntervalUseCase: ObservePollingIntervalUseCase,
    private val setPollingIntervalUseCase: SetPollingIntervalUseCase,
    observeAccentColorUseCase: ObserveAccentColorUseCase,
    private val setAccentColorUseCase: SetAccentColorUseCase,
    observeAmoledDarkModeUseCase: ObserveAmoledDarkModeUseCase,
    private val setAmoledDarkModeUseCase: SetAmoledDarkModeUseCase,
    observeNetworkStatusUseCase: ObserveNetworkStatusUseCase,
    observeOfflineCacheEnabledUseCase: ObserveOfflineCacheEnabledUseCase,
    private val setOfflineCacheEnabledUseCase: SetOfflineCacheEnabledUseCase,
    observeCacheSizeLimitUseCase: ObserveCacheSizeLimitUseCase,
    private val setCacheSizeLimitUseCase: SetCacheSizeLimitUseCase,
    private val getCacheInfoUseCase: GetCacheInfoUseCase,
    private val evictCacheUseCase: EvictCacheUseCase,
) : ViewModel() {

    private val _mutableState = MutableStateFlow(SettingsUiState())

    @Suppress("LongMethod")
    val uiState: StateFlow<SettingsUiState> = combine(
        observeNsfwFilterUseCase(),
        observeDefaultSortOrderUseCase(),
        observeDefaultTimePeriodUseCase(),
        observeGridColumnsUseCase(),
        observeApiKeyUseCase(),
    ) { nsfw, sort, period, columns, apiKey ->
        _mutableState.value.copy(
            nsfwFilterLevel = nsfw,
            defaultSortOrder = sort,
            defaultTimePeriod = period,
            gridColumns = columns,
            apiKey = apiKey,
        )
    }.combine(observePowerUserModeUseCase()) { state, powerUser ->
        state.copy(powerUserMode = powerUser)
    }.combine(observeNotificationsEnabledUseCase()) { state, enabled ->
        state.copy(notificationsEnabled = enabled)
    }.combine(observePollingIntervalUseCase()) { state, interval ->
        state.copy(pollingInterval = interval)
    }.combine(observeAccentColorUseCase()) { state, accent ->
        state.copy(accentColor = accent)
    }.combine(observeAmoledDarkModeUseCase()) { state, amoled ->
        state.copy(amoledDarkMode = amoled)
    }.combine(observeNsfwBlurSettingsUseCase()) { state, blur ->
        state.copy(nsfwBlurSettings = blur)
    }.combine(observeNetworkStatusUseCase()) { state, online ->
        state.copy(isOnline = online)
    }.combine(observeOfflineCacheEnabledUseCase()) { state, enabled ->
        state.copy(offlineCacheEnabled = enabled)
    }.combine(observeCacheSizeLimitUseCase()) { state, limit ->
        state.copy(cacheSizeLimitMb = limit)
    }.combine(_mutableState) { observed, mutable ->
        observed.copy(
            hiddenModels = mutable.hiddenModels,
            excludedTags = mutable.excludedTags,
            connectedUsername = mutable.connectedUsername,
            isValidatingApiKey = mutable.isValidatingApiKey,
            apiKeyError = mutable.apiKeyError,
            cacheInfo = mutable.cacheInfo,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    init {
        loadMutableData()
    }

    private fun loadMutableData() {
        viewModelScope.launch {
            val hidden = getHiddenModelsUseCase()
            val tags = getExcludedTagsUseCase()
            val cacheInfo = getCacheInfoUseCase()
            _mutableState.update {
                it.copy(hiddenModels = hidden, excludedTags = tags, cacheInfo = cacheInfo)
            }
        }
        viewModelScope.launch {
            val key = observeApiKeyUseCase().first() ?: return@launch
            try {
                val username = validateApiKeyUseCase(key)
                _mutableState.update { it.copy(connectedUsername = username) }
            } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
                // Key might be expired
            }
        }
    }

    fun onValidateAndSaveApiKey(apiKey: String) {
        if (apiKey.isBlank()) return
        _mutableState.update { it.copy(isValidatingApiKey = true, apiKeyError = null) }
        viewModelScope.launch {
            try {
                val username = validateApiKeyUseCase(apiKey)
                setApiKeyUseCase(apiKey)
                _mutableState.update {
                    it.copy(connectedUsername = username, isValidatingApiKey = false)
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                _mutableState.update {
                    it.copy(
                        isValidatingApiKey = false,
                        apiKeyError = e.message ?: "Invalid API key",
                    )
                }
            }
        }
    }

    fun onClearApiKey() {
        viewModelScope.launch {
            setApiKeyUseCase(null)
            _mutableState.update {
                it.copy(connectedUsername = null, apiKeyError = null)
            }
        }
    }

    fun onRefreshUsername() {
        viewModelScope.launch {
            val key = observeApiKeyUseCase().first() ?: return@launch
            try {
                val username = validateApiKeyUseCase(key)
                _mutableState.update { it.copy(connectedUsername = username) }
            } catch (@Suppress("TooGenericExceptionCaught") _: Exception) {
                // Silently ignore - key might be expired
            }
        }
    }

    fun onNsfwFilterChanged(level: NsfwFilterLevel) {
        viewModelScope.launch { setNsfwFilterUseCase(level) }
    }

    fun onNsfwBlurSettingsChanged(settings: NsfwBlurSettings) {
        viewModelScope.launch { setNsfwBlurSettingsUseCase(settings) }
    }

    fun onSortOrderChanged(sort: SortOrder) {
        viewModelScope.launch { setDefaultSortOrderUseCase(sort) }
    }

    fun onTimePeriodChanged(period: TimePeriod) {
        viewModelScope.launch { setDefaultTimePeriodUseCase(period) }
    }

    fun onGridColumnsChanged(columns: Int) {
        viewModelScope.launch { setGridColumnsUseCase(columns) }
    }

    fun onPowerUserModeChanged(enabled: Boolean) {
        viewModelScope.launch { setPowerUserModeUseCase(enabled) }
    }

    fun onUnhideModel(modelId: Long) {
        viewModelScope.launch {
            unhideModelUseCase(modelId)
            val hidden = getHiddenModelsUseCase()
            _mutableState.update { it.copy(hiddenModels = hidden) }
        }
    }

    fun onAddExcludedTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            addExcludedTagUseCase(trimmed)
            val tags = getExcludedTagsUseCase()
            _mutableState.update { it.copy(excludedTags = tags) }
        }
    }

    fun onRemoveExcludedTag(tag: String) {
        viewModelScope.launch {
            removeExcludedTagUseCase(tag)
            val tags = getExcludedTagsUseCase()
            _mutableState.update { it.copy(excludedTags = tags) }
        }
    }

    fun onClearSearchHistory() {
        viewModelScope.launch { clearSearchHistoryUseCase() }
    }

    fun onClearBrowsingHistory() {
        viewModelScope.launch { clearBrowsingHistoryUseCase() }
    }

    fun onClearCache() {
        viewModelScope.launch {
            clearCacheUseCase()
            _mutableState.update { it.copy(cacheInfo = getCacheInfoUseCase()) }
        }
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

    fun onAccentColorChanged(color: AccentColor) {
        viewModelScope.launch { setAccentColorUseCase(color) }
    }

    fun onAmoledDarkModeChanged(enabled: Boolean) {
        viewModelScope.launch { setAmoledDarkModeUseCase(enabled) }
    }
    fun onOfflineCacheEnabledChanged(enabled: Boolean) {
        viewModelScope.launch { setOfflineCacheEnabledUseCase(enabled) }
    }

    fun onCacheSizeLimitChanged(limitMb: Int) {
        viewModelScope.launch {
            setCacheSizeLimitUseCase(limitMb)
            evictCacheUseCase(limitMb.toLong() * 1024L * 1024L)
            _mutableState.update { it.copy(cacheInfo = getCacheInfoUseCase()) }
        }
    }
}
