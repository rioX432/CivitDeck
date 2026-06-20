package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
import com.riox432.civitdeck.domain.model.ConnectionSecurityLevel
import com.riox432.civitdeck.domain.model.DiscoveredServer
import com.riox432.civitdeck.domain.model.SecurityLevelHelper
import com.riox432.civitdeck.domain.model.SystemStats
import com.riox432.civitdeck.domain.util.OptimizationSuggestion
import com.riox432.civitdeck.domain.util.generateOptimizationSuggestions
import com.riox432.civitdeck.feature.comfyui.data.NtfySubscriptionService
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ActivateComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchSystemStatsUseCase
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
    val scanError: String? = null,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val systemStats: SystemStats? = null,
    val optimizationSuggestions: List<OptimizationSuggestion> = emptyList(),
    val dismissedSuggestionIds: Set<String> = emptySet(),
    val isNtfySubscribed: Boolean = false,
    val isNtfyTestSending: Boolean = false,
    val ntfyTestResult: Boolean? = null,
)

class ComfyUISettingsViewModel(
    observeConnections: ObserveComfyUIConnectionsUseCase,
    observeActive: ObserveActiveComfyUIConnectionUseCase,
    private val saveConnection: SaveComfyUIConnectionUseCase,
    private val deleteConnection: DeleteComfyUIConnectionUseCase,
    private val activateConnection: ActivateComfyUIConnectionUseCase,
    private val testConnection: TestComfyUIConnectionUseCase,
    private val scanForServers: ScanForServersUseCase,
    private val fetchSystemStats: FetchSystemStatsUseCase,
    private val ntfyService: NtfySubscriptionService,
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

        // Manage ntfy subscription lifecycle based on active connection
        manageNtfySubscription(active, status)

        mutable.copy(
            connections = connections,
            activeConnection = active,
            connectionStatus = status,
            securityLevel = security,
            isNtfySubscribed = ntfyService.isSubscribed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT), ComfyUISettingsUiState())

    @Suppress("LongParameterList")
    fun onSaveConnection(
        name: String,
        hostname: String,
        port: Int,
        useHttps: Boolean = false,
        acceptSelfSigned: Boolean = false,
        ntfyServerUrl: String? = null,
        ntfyTopic: String? = null,
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
                ntfyServerUrl = ntfyServerUrl?.takeIf { it.isNotBlank() },
                ntfyTopic = ntfyTopic?.takeIf { it.isNotBlank() },
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
        _mutableState.update { it.copy(isTesting = true, testError = null, systemStats = null) }
        viewModelScope.launch {
            val success = testConnection(active)
            if (success) {
                val stats = fetchSystemStats()
                val suggestions = stats?.let { generateOptimizationSuggestions(it) } ?: emptyList()
                _mutableState.update {
                    it.copy(
                        isTesting = false,
                        testError = null,
                        systemStats = stats,
                        optimizationSuggestions = suggestions,
                    )
                }
            } else {
                _mutableState.update {
                    it.copy(isTesting = false, testError = "Connection failed", systemStats = null)
                }
            }
        }
    }

    fun onScanLan() {
        scanJob?.cancel()
        _mutableState.update { it.copy(isScanning = true, scanError = null, discoveredServers = emptyList()) }
        scanJob = viewModelScope.launch {
            scanForServers()
                .catch { e ->
                    // Surface scan failures so the user is not left with a silent empty result.
                    _mutableState.update {
                        it.copy(isScanning = false, scanError = e.message ?: e.toString())
                    }
                }
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

    fun dismissSuggestion(id: String) {
        _mutableState.update { it.copy(dismissedSuggestionIds = it.dismissedSuggestionIds + id) }
    }

    fun onTestNtfy() {
        val active = uiState.value.activeConnection ?: return
        val topic = active.ntfyTopic ?: return
        _mutableState.update { it.copy(isNtfyTestSending = true, ntfyTestResult = null) }
        viewModelScope.launch {
            val success = ntfyService.sendTestNotification(
                active.resolvedNtfyServerUrl,
                topic,
            )
            _mutableState.update { it.copy(isNtfyTestSending = false, ntfyTestResult = success) }
        }
    }

    fun clearNtfyTestResult() {
        _mutableState.update { it.copy(ntfyTestResult = null) }
    }

    private fun manageNtfySubscription(
        active: ComfyUIConnection?,
        status: ComfyUIConnectionStatus,
    ) {
        if (active != null &&
            active.isNtfyConfigured &&
            status == ComfyUIConnectionStatus.Connected
        ) {
            ntfyService.subscribe(
                active.resolvedNtfyServerUrl,
                active.ntfyTopic.orEmpty(),
            )
        } else {
            ntfyService.unsubscribe()
        }
    }

    private companion object {
        const val SUBSCRIBE_TIMEOUT = 5_000L
    }
}
