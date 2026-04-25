package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
import com.riox432.civitdeck.domain.model.ConnectionSecurityLevel
import com.riox432.civitdeck.domain.model.DiscoveredServer
import com.riox432.civitdeck.domain.model.SecurityLevelHelper
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ActivateComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveActiveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveComfyUIConnectionsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ScanForServersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.TestComfyUIConnectionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ComfyUISettingsUiState(
    val connections: List<ComfyUIConnection> = emptyList(),
    val activeConnection: ComfyUIConnection? = null,
    val connectionStatus: ComfyUIConnectionStatus = ComfyUIConnectionStatus.NotConfigured,
    val securityLevel: ConnectionSecurityLevel? = null,
    val isTesting: Boolean = false,
    val testError: String? = null,
    val showAddDialog: Boolean = false,
    val editingConnection: ComfyUIConnection? = null,
    val isScanning: Boolean = false,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
)

@Suppress("TooManyFunctions")
class ComfyUISettingsViewModel(
    observeConnections: ObserveComfyUIConnectionsUseCase,
    observeActive: ObserveActiveComfyUIConnectionUseCase,
    private val saveConnection: SaveComfyUIConnectionUseCase,
    private val deleteConnection: DeleteComfyUIConnectionUseCase,
    private val activateConnection: ActivateComfyUIConnectionUseCase,
    private val testConnection: TestComfyUIConnectionUseCase,
    private val scanForServers: ScanForServersUseCase,
) : ViewModel() {

    private val _mutableState = MutableStateFlow(ComfyUISettingsUiState())
    private var scanJob: Job? = null

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
        val security = active?.let { SecurityLevelHelper.getSecurityLevel(it) }
        mutable.copy(
            connections = connections,
            activeConnection = active,
            connectionStatus = status,
            securityLevel = security,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT), ComfyUISettingsUiState())

    fun onSaveConnection(
        name: String,
        hostname: String,
        port: Int,
        useHttps: Boolean = false,
        acceptSelfSigned: Boolean = false,
    ) {
        viewModelScope.launch {
            val editing = _mutableState.value.editingConnection
            val connection = ComfyUIConnection(
                id = editing?.id ?: 0,
                name = name,
                hostname = hostname,
                port = port,
                useHttps = useHttps,
                acceptSelfSigned = acceptSelfSigned,
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

    fun onScanLan() {
        scanJob?.cancel()
        _mutableState.update { it.copy(isScanning = true, discoveredServers = emptyList()) }
        scanJob = viewModelScope.launch {
            scanForServers()
                .catch { _mutableState.update { it.copy(isScanning = false) } }
                .collect { servers ->
                    _mutableState.update { it.copy(discoveredServers = servers) }
                }
            _mutableState.update { it.copy(isScanning = false) }
        }
    }

    fun onSelectDiscoveredServer(server: DiscoveredServer) {
        onSaveConnection(
            name = server.displayName,
            hostname = server.ip,
            port = server.port,
        )
        _mutableState.update { it.copy(discoveredServers = emptyList(), isScanning = false) }
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

    private companion object {
        const val SUBSCRIBE_TIMEOUT = 5_000L
    }
}
