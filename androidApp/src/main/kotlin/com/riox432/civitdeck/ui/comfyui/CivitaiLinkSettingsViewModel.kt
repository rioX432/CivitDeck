package com.riox432.civitdeck.ui.comfyui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.CivitaiLinkActivity
import com.riox432.civitdeck.domain.model.CivitaiLinkStatus
import com.riox432.civitdeck.domain.usecase.ObserveCivitaiLinkKeyUseCase
import com.riox432.civitdeck.domain.usecase.SetCivitaiLinkKeyUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.CancelLinkActivityUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ConnectCivitaiLinkUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DisconnectCivitaiLinkUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveCivitaiLinkActivitiesUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveCivitaiLinkStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CivitaiLinkSettingsUiState(
    val linkKey: String = "",
    val status: CivitaiLinkStatus = CivitaiLinkStatus.Disconnected,
    val activities: List<CivitaiLinkActivity> = emptyList(),
    val isSaving: Boolean = false,
)

class CivitaiLinkSettingsViewModel(
    observeKey: ObserveCivitaiLinkKeyUseCase,
    observeStatus: ObserveCivitaiLinkStatusUseCase,
    observeActivities: ObserveCivitaiLinkActivitiesUseCase,
    private val setKey: SetCivitaiLinkKeyUseCase,
    private val connect: ConnectCivitaiLinkUseCase,
    private val disconnect: DisconnectCivitaiLinkUseCase,
    private val cancelActivity: CancelLinkActivityUseCase,
) : ViewModel() {

    private val _mutable = MutableStateFlow(CivitaiLinkSettingsUiState())

    val uiState: StateFlow<CivitaiLinkSettingsUiState> = combine(
        observeKey(),
        observeStatus(),
        observeActivities(),
        _mutable,
    ) { key, status, activities, mutable ->
        mutable.copy(
            linkKey = key ?: mutable.linkKey,
            status = status,
            activities = activities,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CivitaiLinkSettingsUiState())

    fun onKeyChanged(key: String) {
        _mutable.update { it.copy(linkKey = key) }
    }

    fun onSaveAndConnect() {
        val key = _mutable.value.linkKey.trim()
        if (key.isEmpty()) return
        _mutable.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            setKey(key)
            connect()
            _mutable.update { it.copy(isSaving = false) }
        }
    }

    fun onDisconnect() {
        disconnect()
    }

    fun onCancelActivity(activityId: String) {
        viewModelScope.launch { cancelActivity(activityId) }
    }
}
