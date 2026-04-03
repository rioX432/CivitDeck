package com.riox432.civitdeck.ui.comfyui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.SDWebUIConnection
import com.riox432.civitdeck.domain.model.SDWebUIConnectionStatus
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ActivateSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveActiveSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveSDWebUIConnectionsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveSDWebUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.TestSDWebUIConnectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SDWebUISettingsUiState(
    val connections: List<SDWebUIConnection> = emptyList(),
    val activeConnection: SDWebUIConnection? = null,
    val connectionStatus: SDWebUIConnectionStatus = SDWebUIConnectionStatus.NotConfigured,
    val isTesting: Boolean = false,
    val testError: String? = null,
    val showAddDialog: Boolean = false,
    val editingConnection: SDWebUIConnection? = null,
)

class SDWebUISettingsViewModel(
    observeConnections: ObserveSDWebUIConnectionsUseCase,
    observeActive: ObserveActiveSDWebUIConnectionUseCase,
    private val saveConnection: SaveSDWebUIConnectionUseCase,
    private val deleteConnection: DeleteSDWebUIConnectionUseCase,
    private val activateConnection: ActivateSDWebUIConnectionUseCase,
    private val testConnection: TestSDWebUIConnectionUseCase,
) : ViewModel() {

    private val _mutable = MutableStateFlow(SDWebUISettingsUiState())

    val uiState: StateFlow<SDWebUISettingsUiState> = combine(
        observeConnections(),
        observeActive(),
        _mutable,
    ) { connections, active, mutable ->
        val status = when {
            active == null -> SDWebUIConnectionStatus.NotConfigured
            mutable.isTesting -> SDWebUIConnectionStatus.Testing
            active.lastTestSuccess == true -> SDWebUIConnectionStatus.Connected
            active.lastTestSuccess == false -> SDWebUIConnectionStatus.Error
            else -> SDWebUIConnectionStatus.Disconnected
        }
        mutable.copy(connections = connections, activeConnection = active, connectionStatus = status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SDWebUISettingsUiState())

    fun onSaveConnection(name: String, hostname: String, port: Int) {
        viewModelScope.launch {
            val editing = _mutable.value.editingConnection
            val conn = SDWebUIConnection(
                id = editing?.id ?: 0,
                name = name,
                hostname = hostname,
                port = port,
            )
            saveConnection(conn)
            _mutable.update { it.copy(showAddDialog = false, editingConnection = null) }
        }
    }

    fun onDeleteConnection(id: Long) { viewModelScope.launch { deleteConnection(id) } }

    fun onActivateConnection(id: Long) { viewModelScope.launch { activateConnection(id) } }

    fun onTestConnection() {
        val active = uiState.value.activeConnection ?: return
        _mutable.update { it.copy(isTesting = true, testError = null) }
        viewModelScope.launch {
            val success = testConnection(active)
            _mutable.update {
                it.copy(isTesting = false, testError = if (success) null else "Connection failed")
            }
        }
    }

    fun onShowAddDialog() {
        _mutable.update { it.copy(showAddDialog = true, editingConnection = null) }
    }

    fun onEditConnection(conn: SDWebUIConnection) {
        _mutable.update { it.copy(showAddDialog = true, editingConnection = conn) }
    }

    fun onDismissDialog() {
        _mutable.update { it.copy(showAddDialog = false, editingConnection = null) }
    }
}
