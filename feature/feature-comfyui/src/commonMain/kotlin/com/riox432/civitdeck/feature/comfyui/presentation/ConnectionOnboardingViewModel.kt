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

    /**
     * LAN scan (Android/Desktop only); [results] grows as servers respond. [isComplete] turns true
     * once the scan has finished or failed, keeping the results found so far.
     */
    data class Scanning(
        val results: List<DiscoveredServer>,
        val isComplete: Boolean = false,
    ) : OnboardingStep

    /** A connection is being verified against the live server. */
    data class Testing(val connection: ComfyUIConnection) : OnboardingStep

    /** The connection succeeded and was persisted. */
    data class Success(val connection: ComfyUIConnection, val stats: SystemStats?) : OnboardingStep

    /**
     * The connection failed; [cause] gives an actionable hint. [presentedSha256] is the
     * fingerprint the server presented when [cause] asks the user to confirm a certificate.
     */
    data class Failure(
        val connection: ComfyUIConnection,
        val cause: ConnectionFailureCause,
        val httpStatus: Int?,
        val presentedSha256: String? = null,
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
                .catch { /* a failed scan counts as finished; keep last results */ }
                .collect { servers -> updateScanning { it.copy(results = servers) } }
            // Skipped when scanJob is cancelled (catch rethrows the job's own cancellation): the user
            // has left this step. Any other upstream failure, even a foreign CancellationException,
            // ends the scan while the user still waits on it, so it counts as finished.
            updateScanning { it.copy(isComplete = true) }
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

    /**
     * Tests and (on success) saves a manually entered connection. [hostname] may be a bare host or
     * a pasted URL; a scheme or port written in it wins over [port] and [useHttps].
     */
    fun onManualSubmit(
        name: String,
        hostname: String,
        port: Int,
        useHttps: Boolean,
        acceptSelfSigned: Boolean,
    ) {
        val parsed = parseConnectionUrl.parseManualEntry(hostname, port, useHttps) ?: run {
            failWithUnknown(
                ComfyUIConnection(
                    name = name.ifBlank { hostname },
                    hostname = hostname,
                    port = port,
                    useHttps = useHttps,
                    acceptSelfSigned = acceptSelfSigned,
                ),
            )
            return
        }
        testAndSave(
            parsed.copy(
                name = name.ifBlank { parsed.hostname },
                acceptSelfSigned = acceptSelfSigned,
            ),
        )
    }

    /** Retries the most recent failed connection. */
    fun onRetry() {
        val failure = _uiState.value.step as? OnboardingStep.Failure ?: return
        testAndSave(failure.connection)
    }

    /**
     * Trusts the certificate the server presented in the current certificate failure: tests again
     * with its fingerprint as the pin and, on success, saves the connection with that pin.
     * No-op unless the current step is such a failure.
     */
    fun onTrustCertificate() {
        val failure = _uiState.value.step as? OnboardingStep.Failure ?: return
        if (failure.cause !in CERTIFICATE_CAUSES) return
        val presented = failure.presentedSha256 ?: return
        testAndSave(failure.connection.copy(tlsCertSha256 = presented))
    }

    /**
     * Re-tests a saved connection with its stored pin, so a changed or unconfirmed certificate
     * surfaces as a failure the user can confirm with [onTrustCertificate].
     */
    fun onReviewCertificate(saved: ComfyUIConnection) {
        scanJob?.cancel()
        testAndSave(saved)
    }

    private fun testAndSave(connection: ComfyUIConnection) {
        testJob?.cancel()
        _uiState.update { it.copy(step = OnboardingStep.Testing(connection)) }
        testJob = viewModelScope.launch {
            when (val result = connectionTester.test(connection)) {
                is ConnectionTestResult.Success -> persist(connection, result.stats)
                is ConnectionTestResult.Failure -> _uiState.update {
                    it.copy(
                        step = OnboardingStep.Failure(
                            connection = connection,
                            cause = result.cause,
                            httpStatus = result.httpStatus,
                            presentedSha256 = result.presentedSha256,
                        ),
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

    /** Applies [transform] only while the step is still [OnboardingStep.Scanning]. */
    private fun updateScanning(transform: (OnboardingStep.Scanning) -> OnboardingStep.Scanning) {
        _uiState.update { state ->
            val step = state.step
            if (step is OnboardingStep.Scanning) state.copy(step = transform(step)) else state
        }
    }

    private fun failWithUnknown(connection: ComfyUIConnection) {
        _uiState.update {
            it.copy(step = OnboardingStep.Failure(connection, ConnectionFailureCause.Unknown, null))
        }
    }

    private companion object {
        val CERTIFICATE_CAUSES = setOf(
            ConnectionFailureCause.CertificateUnconfirmed,
            ConnectionFailureCause.CertificateChanged,
        )
    }
}
