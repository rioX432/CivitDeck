package com.riox432.civitdeck.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.CacheInfo
import com.riox432.civitdeck.domain.model.HiddenModel
import com.riox432.civitdeck.domain.usecase.ClearBrowsingHistoryUseCase
import com.riox432.civitdeck.domain.usecase.ClearCacheUseCase
import com.riox432.civitdeck.domain.usecase.ClearSearchHistoryUseCase
import com.riox432.civitdeck.domain.usecase.EvictCacheUseCase
import com.riox432.civitdeck.domain.usecase.GetCacheInfoUseCase
import com.riox432.civitdeck.domain.usecase.GetHiddenModelsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNetworkStatusUseCase
import com.riox432.civitdeck.domain.usecase.ObserveOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.SetCacheSizeLimitUseCase
import com.riox432.civitdeck.domain.usecase.SetOfflineCacheEnabledUseCase
import com.riox432.civitdeck.domain.usecase.UnhideModelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StorageSettingsUiState(
    val isOnline: Boolean = true,
    val offlineCacheEnabled: Boolean = true,
    val cacheSizeLimitMb: Int = 200,
    val cacheInfo: CacheInfo = CacheInfo(0, 0),
    val hiddenModels: List<HiddenModel> = emptyList(),
)

@Suppress("LongParameterList")
class StorageSettingsViewModel(
    observeNetworkStatusUseCase: ObserveNetworkStatusUseCase,
    observeOfflineCacheEnabledUseCase: ObserveOfflineCacheEnabledUseCase,
    private val setOfflineCacheEnabledUseCase: SetOfflineCacheEnabledUseCase,
    observeCacheSizeLimitUseCase: ObserveCacheSizeLimitUseCase,
    private val setCacheSizeLimitUseCase: SetCacheSizeLimitUseCase,
    private val getCacheInfoUseCase: GetCacheInfoUseCase,
    private val evictCacheUseCase: EvictCacheUseCase,
    private val clearSearchHistoryUseCase: ClearSearchHistoryUseCase,
    private val clearBrowsingHistoryUseCase: ClearBrowsingHistoryUseCase,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val getHiddenModelsUseCase: GetHiddenModelsUseCase,
    private val unhideModelUseCase: UnhideModelUseCase,
) : ViewModel() {

    private val _mutableState = MutableStateFlow(StorageSettingsUiState())

    val uiState: StateFlow<StorageSettingsUiState> = combine(
        observeNetworkStatusUseCase(),
        observeOfflineCacheEnabledUseCase(),
        observeCacheSizeLimitUseCase(),
        _mutableState,
    ) { online, cacheEnabled, cacheLimit, mutable ->
        mutable.copy(
            isOnline = online,
            offlineCacheEnabled = cacheEnabled,
            cacheSizeLimitMb = cacheLimit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StorageSettingsUiState())

    init {
        viewModelScope.launch {
            val cacheInfo = getCacheInfoUseCase()
            val hidden = getHiddenModelsUseCase()
            _mutableState.update { it.copy(cacheInfo = cacheInfo, hiddenModels = hidden) }
        }
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

    fun onUnhideModel(modelId: Long) {
        viewModelScope.launch {
            unhideModelUseCase(modelId)
            val hidden = getHiddenModelsUseCase()
            _mutableState.update { it.copy(hiddenModels = hidden) }
        }
    }
}
