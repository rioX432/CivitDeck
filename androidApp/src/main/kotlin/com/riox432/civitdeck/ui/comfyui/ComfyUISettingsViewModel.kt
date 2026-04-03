package com.riox432.civitdeck.ui.comfyui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ActivateComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveActiveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveComfyUIConnectionsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.TestComfyUIConnectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ComfyUISettingsUiState(
    val connections: List<ComfyUIConnection> = emptyList(),
    val activeConnection: ComfyUIConnection? = null,
    val connectionStatus: ComfyUIConnectionStatus = ComfyUIConnectionStatus.NotConfigured,
    val isTesting: Boolean = false,
    val testError: String? = null,
    val showAddDialog: Boolean = false,
    val editingConnection: ComfyUIConnection? = null,
)

class ComfyUISettingsViewModel(
    observeConnections: ObserveComfyUIConnectionsUseCase,
    observeActive: ObserveActiveComfyUIConnectionUseCase,
    private val saveConnection: SaveComfyUIConnectionUseCase,
    private val deleteConnection: DeleteComfyUIConnectionUseCase,
    private val activateConnection: ActivateComfyUIConnectionUseCase,
    private val testConnection: TestComfyUIConnectionUseCase,
) : ViewModel() {

    private val _mutableState = MutableStateFlow(ComfyUISettingsUiState())

    val uiState: StateFlow<ComfyUISettingsUiState> = combine(
        observeConnections(),
        observeActive(),
        _mutableState,
    ) { connections, active, mutable ->
        val status = when {
            active == null -> ComfyUIConnectionStatus.NotConfigured
            mutable.isTesting -> ComfyUIConnectionStatus.Testing
            active.lastTestSuccess == true -> ComfyUIConnectionStatus.Connected
            active.lastTestSuccess == false -> ComfyUIConnectionStatus.Error
            else -> ComfyUIConnectionStatus.Disconnected
        }
        mutable.copy(
            connections = connections,
            activeConnection = active,
            connectionStatus = status,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ComfyUISettingsUiState())

    fun onSaveConnection(name: String, hostname: String, port: Int) {
        viewModelScope.launch {
            val editing = _mutableState.value.editingConnection
            val connection = ComfyUIConnection(
                id = editing?.id ?: 0,
                name = name,
                hostname = hostname,
                port = port,
            )
            saveConnection(connection)
            _mutableState.update { it.copy(showAddDialog = false, editingConnection = null) }
        }
    }

    fun onDeleteConnection(id: Long) {
        viewModelScope.launch { deleteConnection(id) }
    }

    fun onActivateConnection(id: Long) {
        viewModelScope.launch { activateConnection(id) }
    }

    fun onTestConnection() {
        val active = uiState.value.activeConnection ?: return
        _mutableState.update { it.copy(isTesting = true, testError = null) }
        viewModelScope.launch {
            val success = testConnection(active)
            _mutableState.update {
                it.copy(
                    isTesting = false,
                    testError = if (success) null else "Connection failed",
                )
            }
        }
    }

    fun onShowAddDialog() {
        _mutableState.update { it.copy(showAddDialog = true, editingConnection = null) }
    }

    fun onEditConnection(connection: ComfyUIConnection) {
        _mutableState.update { it.copy(showAddDialog = true, editingConnection = connection) }
    }

    fun onDismissDialog() {
        _mutableState.update { it.copy(showAddDialog = false, editingConnection = null) }
    }
}
