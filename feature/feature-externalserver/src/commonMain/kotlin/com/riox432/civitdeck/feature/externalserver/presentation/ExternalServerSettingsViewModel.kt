package com.riox432.civitdeck.feature.externalserver.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ExternalServerConfig
import com.riox432.civitdeck.domain.model.ExternalServerConnectionStatus
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ActivateExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.DeleteExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ObserveActiveExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ObserveExternalServerConfigsUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.SaveExternalServerConfigUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.TestExternalServerConnectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExternalServerSettingsUiState(
    val configs: List<ExternalServerConfig> = emptyList(),
    val activeConfig: ExternalServerConfig? = null,
    val connectionStatus: ExternalServerConnectionStatus = ExternalServerConnectionStatus.NotConfigured,
    val isTesting: Boolean = false,
    val testError: String? = null,
    val showAddDialog: Boolean = false,
    val editingConfig: ExternalServerConfig? = null,
)

class ExternalServerSettingsViewModel(
    observeConfigs: ObserveExternalServerConfigsUseCase,
    observeActive: ObserveActiveExternalServerConfigUseCase,
    private val saveConfig: SaveExternalServerConfigUseCase,
    private val deleteConfig: DeleteExternalServerConfigUseCase,
    private val activateConfig: ActivateExternalServerConfigUseCase,
    private val testConnection: TestExternalServerConnectionUseCase,
) : ViewModel() {

    private val _mutableState = MutableStateFlow(ExternalServerSettingsUiState())

    val uiState: StateFlow<ExternalServerSettingsUiState> = combine(
        observeConfigs(),
        observeActive(),
        _mutableState,
    ) { configs, active, mutable ->
        val status = when {
            active == null -> ExternalServerConnectionStatus.NotConfigured
            mutable.isTesting -> ExternalServerConnectionStatus.Testing
            active.lastTestSuccess == true -> ExternalServerConnectionStatus.Connected
            active.lastTestSuccess == false -> ExternalServerConnectionStatus.Error
            else -> ExternalServerConnectionStatus.Disconnected
        }
        mutable.copy(
            configs = configs,
            activeConfig = active,
            connectionStatus = status,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExternalServerSettingsUiState())

    fun onShowAddDialog() {
        _mutableState.update { it.copy(showAddDialog = true, editingConfig = null) }
    }

    fun onEditConfig(config: ExternalServerConfig) {
        _mutableState.update { it.copy(showAddDialog = true, editingConfig = config) }
    }

    fun onDismissDialog() {
        _mutableState.update { it.copy(showAddDialog = false, editingConfig = null) }
    }

    fun onSaveConfig(name: String, baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            val editing = _mutableState.value.editingConfig
            val config = ExternalServerConfig(
                id = editing?.id ?: 0L,
                name = name,
                baseUrl = baseUrl,
                apiKey = apiKey,
                createdAt = editing?.createdAt ?: 0L,
            )
            saveConfig(config)
            _mutableState.update { it.copy(showAddDialog = false, editingConfig = null) }
        }
    }

    fun onDeleteConfig(id: Long) {
        viewModelScope.launch { deleteConfig(id) }
    }

    fun onActivateConfig(id: Long) {
        viewModelScope.launch { activateConfig(id) }
    }

    fun onTestConnection() {
        val active = uiState.value.activeConfig ?: return
        viewModelScope.launch {
            _mutableState.update { it.copy(isTesting = true, testError = null) }
            val success = testConnection(active)
            _mutableState.update {
                it.copy(
                    isTesting = false,
                    testError = if (!success) "Connection failed. Check the server URL and API key." else null,
                )
            }
        }
    }
}
