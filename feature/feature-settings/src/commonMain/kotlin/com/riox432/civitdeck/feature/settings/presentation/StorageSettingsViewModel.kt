package com.riox432.civitdeck.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.CacheInfo
import com.riox432.civitdeck.domain.model.HiddenModel
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

class StorageSettingsViewModel(
    private val cacheUseCases: CacheUseCases,
    private val storedDataUseCases: StoredDataUseCases,
) : ViewModel() {

    private val _mutableState = MutableStateFlow(StorageSettingsUiState())

    val uiState: StateFlow<StorageSettingsUiState> = combine(
        cacheUseCases.observeNetworkStatus(),
        cacheUseCases.observeOfflineCacheEnabled(),
        cacheUseCases.observeCacheSizeLimit(),
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
            val cacheInfo = cacheUseCases.getCacheInfo()
            val hidden = storedDataUseCases.getHiddenModels()
            _mutableState.update { it.copy(cacheInfo = cacheInfo, hiddenModels = hidden) }
        }
    }

    fun onOfflineCacheEnabledChanged(enabled: Boolean) {
        viewModelScope.launch { cacheUseCases.setOfflineCacheEnabled(enabled) }
    }

    fun onCacheSizeLimitChanged(limitMb: Int) {
        viewModelScope.launch {
            cacheUseCases.setCacheSizeLimit(limitMb)
            cacheUseCases.evictCache(limitMb.toLong() * 1024L * 1024L)
            _mutableState.update { it.copy(cacheInfo = cacheUseCases.getCacheInfo()) }
        }
    }

    fun onClearSearchHistory() {
        viewModelScope.launch { storedDataUseCases.clearSearchHistory() }
    }

    fun onClearBrowsingHistory() {
        viewModelScope.launch { storedDataUseCases.clearBrowsingHistory() }
    }

    fun onClearCache() {
        viewModelScope.launch {
            cacheUseCases.clearCache()
            _mutableState.update { it.copy(cacheInfo = cacheUseCases.getCacheInfo()) }
        }
    }

    fun onUnhideModel(modelId: Long) {
        viewModelScope.launch {
            storedDataUseCases.unhideModel(modelId)
            val hidden = storedDataUseCases.getHiddenModels()
            _mutableState.update { it.copy(hiddenModels = hidden) }
        }
    }
}
