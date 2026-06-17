package com.riox432.civitdeck.feature.comfyui.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ConnectionFailureCause
import com.riox432.civitdeck.domain.model.ConnectionTestResult
import com.riox432.civitdeck.domain.model.DiscoveredServer
import com.riox432.civitdeck.domain.model.SystemStats
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionTester
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ActivateComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ParseConnectionUrlUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ScanForServersUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Steps of the guided ComfyUI connection onboarding. A single sealed step prevents
 * invalid combinations (e.g. scanning and testing at once) that boolean flags allow.
 */
sealed interface OnboardingStep {
    /** Entry point: the user picks detect / QR / manual. */
    data object ChooseMethod : OnboardingStep

    /** LAN scan in progress (Android/Desktop only); [results] grows as servers respond. */
    data class Scanning(val results: List<DiscoveredServer>) : OnboardingStep

    /** A connection is being verified against the live server. */
    data class Testing(val connection: ComfyUIConnection) : OnboardingStep

    /** The connection succeeded and was persisted. */
    data class Success(val connection: ComfyUIConnection, val stats: SystemStats?) : OnboardingStep

    /** The connection failed; [cause] gives an actionable hint. */
    data class Failure(
        val connection: ComfyUIConnection,
        val cause: ConnectionFailureCause,
        val httpStatus: Int?,
    ) : OnboardingStep
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.ChooseMethod,
    val lanScanSupported: Boolean = false,
)

/**
 * Orchestrates connection onboarding: detect (Android/Desktop) → QR → manual, each
 * followed by a connection test (health check + optional stats) and persistence.
 */
class ConnectionOnboardingViewModel(
    private val scanForServers: ScanForServersUseCase,
    private val connectionTester: ComfyUIConnectionTester,
    private val parseConnectionUrl: ParseConnectionUrlUseCase,
    private val saveConnection: SaveComfyUIConnectionUseCase,
    private val activateConnection: ActivateComfyUIConnectionUseCase,
    private val lanScanSupported: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState(lanScanSupported = lanScanSupported))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null
    private var testJob: Job? = null

    /** Returns to the method picker, cancelling any in-flight work. */
    fun onChooseMethod() {
        scanJob?.cancel()
        testJob?.cancel()
        _uiState.update { it.copy(step = OnboardingStep.ChooseMethod) }
    }

    /** Starts a cancellable LAN scan. No-op when the platform cannot scan reliably. */
    fun onStartScan() {
        if (!lanScanSupported) return
        scanJob?.cancel()
        _uiState.update { it.copy(step = OnboardingStep.Scanning(emptyList())) }
        scanJob = viewModelScope.launch {
            scanForServers()
                .catch { /* scan finished or failed; keep last results */ }
                .collect { servers ->
                    _uiState.update { state ->
                        if (state.step is OnboardingStep.Scanning) {
                            state.copy(step = OnboardingStep.Scanning(servers))
                        } else {
                            state
                        }
                    }
                }
        }
    }

    /** Tests and (on success) saves a server discovered via LAN scan. */
    fun onSelectDiscoveredServer(server: DiscoveredServer) {
        scanJob?.cancel()
        testAndSave(
            ComfyUIConnection(
                name = server.displayName,
                hostname = server.ip,
                port = server.port,
            ),
        )
    }

    /** Tests and (on success) saves a connection parsed from a scanned QR payload. */
    fun onQrScanned(raw: String) {
        val connection = parseConnectionUrl(raw) ?: run {
            failWithUnknown(ComfyUIConnection(name = raw, hostname = raw))
            return
        }
        testAndSave(connection)
    }

    /** Tests and (on success) saves a manually entered connection. */
    @Suppress("LongParameterList")
    fun onManualSubmit(
        name: String,
        hostname: String,
        port: Int,
        useHttps: Boolean,
        acceptSelfSigned: Boolean,
    ) {
        testAndSave(
            ComfyUIConnection(
                name = name.ifBlank { hostname },
                hostname = hostname,
                port = port,
                useHttps = useHttps,
                acceptSelfSigned = acceptSelfSigned,
            ),
        )
    }

    /** Retries the most recent failed connection. */
    fun onRetry() {
        val failure = _uiState.value.step as? OnboardingStep.Failure ?: return
        testAndSave(failure.connection)
    }

    private fun testAndSave(connection: ComfyUIConnection) {
        testJob?.cancel()
        _uiState.update { it.copy(step = OnboardingStep.Testing(connection)) }
        testJob = viewModelScope.launch {
            when (val result = connectionTester.test(connection)) {
                is ConnectionTestResult.Success -> persist(connection, result.stats)
                is ConnectionTestResult.Failure -> _uiState.update {
                    it.copy(
                        step = OnboardingStep.Failure(connection, result.cause, result.httpStatus),
                    )
                }
            }
        }
    }

    private suspend fun persist(connection: ComfyUIConnection, stats: SystemStats?) {
        val id = saveConnection(connection.copy(lastTestSuccess = true))
        activateConnection(id)
        _uiState.update {
            it.copy(step = OnboardingStep.Success(connection.copy(id = id), stats))
        }
    }

    private fun failWithUnknown(connection: ComfyUIConnection) {
        _uiState.update {
            it.copy(step = OnboardingStep.Failure(connection, ConnectionFailureCause.Unknown, null))
        }
    }
}
