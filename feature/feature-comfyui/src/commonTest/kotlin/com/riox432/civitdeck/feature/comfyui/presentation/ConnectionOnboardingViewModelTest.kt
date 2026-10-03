package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ConnectionFailureCause
import com.riox432.civitdeck.domain.model.ConnectionTestResult
import com.riox432.civitdeck.domain.model.DiscoveredServer
import com.riox432.civitdeck.domain.model.SystemStats
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionTester
import com.riox432.civitdeck.domain.repository.ServerDiscoveryRepository
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ActivateComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ParseConnectionUrlUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ScanForServersUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionOnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun scanning_emits_discovered_servers() = runTest(dispatcher) {
        val server = DiscoveredServer("host", "192.168.1.10", DEFAULT_PORT, "ComfyUI @ host")
        val vm = createViewModel(discovery = FakeDiscovery(listOf(server)))

        vm.onStartScan()
        dispatcher.scheduler.advanceUntilIdle()

        val step = vm.uiState.value.step
        assertIs<OnboardingStep.Scanning>(step)
        assertEquals(listOf(server), step.results)
    }

    @Test
    fun scan_hidden_when_lan_unsupported() = runTest(dispatcher) {
        val vm = createViewModel(lanScanSupported = false)

        vm.onStartScan()
        dispatcher.scheduler.advanceUntilIdle()

        // onStartScan is a no-op; remains on ChooseMethod.
        assertIs<OnboardingStep.ChooseMethod>(vm.uiState.value.step)
        assertEquals(false, vm.uiState.value.lanScanSupported)
    }

    @Test
    fun select_discovered_server_tests_then_saves_and_activates() = runTest(dispatcher) {
        val stats = systemStats()
        val repo = FakeConnectionRepository()
        val tester = FakeTester(ConnectionTestResult.Success(stats))
        val vm = createViewModel(repository = repo, tester = tester)

        vm.onSelectDiscoveredServer(DiscoveredServer("h", "10.0.0.5", DEFAULT_PORT, "ComfyUI"))
        dispatcher.scheduler.advanceUntilIdle()

        val step = vm.uiState.value.step
        assertIs<OnboardingStep.Success>(step)
        assertEquals(stats, step.stats)
        assertTrue(step.connection.id != 0L)
        assertEquals(1, repo.saved.size)
        assertEquals(step.connection.id, repo.activatedId)
        assertEquals(true, repo.saved.first().lastTestSuccess)
    }

    @Test
    fun manual_submit_failure_surfaces_cause() = runTest(dispatcher) {
        val tester = FakeTester(ConnectionTestResult.Failure(ConnectionFailureCause.Tls))
        val repo = FakeConnectionRepository()
        val vm = createViewModel(repository = repo, tester = tester)

        vm.onManualSubmit("My PC", "comfy.local", DEFAULT_PORT, useHttps = true, acceptSelfSigned = true)
        dispatcher.scheduler.advanceUntilIdle()

        val step = vm.uiState.value.step
        assertIs<OnboardingStep.Failure>(step)
        assertEquals(ConnectionFailureCause.Tls, step.cause)
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun manual_submit_with_pasted_http_url_saves_its_host_port_and_scheme() = runTest(dispatcher) {
        val repo = FakeConnectionRepository()
        val vm = createViewModel(repository = repo)

        vm.onManualSubmit("", "http://192.168.1.5:8188/", HTTPS_PORT, useHttps = true, acceptSelfSigned = false)
        dispatcher.scheduler.advanceUntilIdle()

        assertIs<OnboardingStep.Success>(vm.uiState.value.step)
        val saved = repo.saved.single()
        assertEquals("192.168.1.5", saved.hostname)
        assertEquals("192.168.1.5", saved.name)
        assertEquals(DEFAULT_PORT, saved.port)
        assertEquals(false, saved.useHttps)
    }

    @Test
    fun manual_submit_with_pasted_https_url_saves_https_default_port() = runTest(dispatcher) {
        val repo = FakeConnectionRepository()
        val vm = createViewModel(repository = repo)

        vm.onManualSubmit("PC", "https://pc.tailnet.ts.net", DEFAULT_PORT, useHttps = false, acceptSelfSigned = true)
        dispatcher.scheduler.advanceUntilIdle()

        assertIs<OnboardingStep.Success>(vm.uiState.value.step)
        val saved = repo.saved.single()
        assertEquals("pc.tailnet.ts.net", saved.hostname)
        assertEquals("PC", saved.name)
        assertEquals(HTTPS_PORT, saved.port)
        assertEquals(true, saved.useHttps)
        assertEquals(true, saved.acceptSelfSigned)
    }

    @Test
    fun manual_submit_with_unparseable_host_fails_unknown_without_testing() = runTest(dispatcher) {
        val repo = FakeConnectionRepository()
        val tester = FakeTester(ConnectionTestResult.Success(null))
        val vm = createViewModel(repository = repo, tester = tester)

        vm.onManualSubmit("PC", "host:abc", DEFAULT_PORT, useHttps = false, acceptSelfSigned = false)
        dispatcher.scheduler.advanceUntilIdle()

        val step = vm.uiState.value.step
        assertIs<OnboardingStep.Failure>(step)
        assertEquals(ConnectionFailureCause.Unknown, step.cause)
        assertTrue(tester.tested.isEmpty())
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun qr_scan_with_invalid_payload_fails_unknown() = runTest(dispatcher) {
        val vm = createViewModel()

        vm.onQrScanned("   ")
        dispatcher.scheduler.advanceUntilIdle()

        val step = vm.uiState.value.step
        assertIs<OnboardingStep.Failure>(step)
        assertEquals(ConnectionFailureCause.Unknown, step.cause)
    }

    @Test
    fun qr_scan_parses_url_then_tests_and_saves() = runTest(dispatcher) {
        val repo = FakeConnectionRepository()
        val vm = createViewModel(repository = repo, tester = FakeTester(ConnectionTestResult.Success(null)))

        vm.onQrScanned("http://192.168.1.20:8188")
        dispatcher.scheduler.advanceUntilIdle()

        assertIs<OnboardingStep.Success>(vm.uiState.value.step)
        assertEquals("192.168.1.20", repo.saved.first().hostname)
        assertEquals(DEFAULT_PORT, repo.saved.first().port)
    }

    @Test
    fun retry_after_failure_can_succeed() = runTest(dispatcher) {
        val repo = FakeConnectionRepository()
        val tester = FakeTester(ConnectionTestResult.Failure(ConnectionFailureCause.Timeout))
        val vm = createViewModel(repository = repo, tester = tester)

        vm.onManualSubmit("PC", "10.0.0.9", DEFAULT_PORT, useHttps = false, acceptSelfSigned = false)
        dispatcher.scheduler.advanceUntilIdle()
        assertIs<OnboardingStep.Failure>(vm.uiState.value.step)

        tester.result = ConnectionTestResult.Success(null)
        vm.onRetry()
        dispatcher.scheduler.advanceUntilIdle()

        assertIs<OnboardingStep.Success>(vm.uiState.value.step)
    }

    @Test
    fun unconfirmed_certificate_then_trust_saves_connection_with_the_presented_pin() = runTest(dispatcher) {
        val repo = FakeConnectionRepository()
        val tester = PinningFakeTester(serverSha256 = SERVER_PIN)
        val vm = createViewModel(repository = repo, tester = tester)

        vm.onManualSubmit("PC", "10.0.0.9", DEFAULT_PORT, useHttps = true, acceptSelfSigned = true)
        dispatcher.scheduler.advanceUntilIdle()

        val failure = vm.uiState.value.step
        assertIs<OnboardingStep.Failure>(failure)
        assertEquals(ConnectionFailureCause.CertificateUnconfirmed, failure.cause)
        assertEquals(SERVER_PIN, failure.presentedSha256)
        assertTrue(repo.saved.isEmpty())

        vm.onTrustCertificate()
        dispatcher.scheduler.advanceUntilIdle()

        val success = vm.uiState.value.step
        assertIs<OnboardingStep.Success>(success)
        assertEquals(SERVER_PIN, tester.tested.last().tlsCertSha256)
        assertEquals(1, repo.saved.size)
        assertEquals(SERVER_PIN, repo.saved.single().tlsCertSha256)
        assertEquals(success.connection.id, repo.activatedId)
    }

    @Test
    fun review_of_saved_connection_with_changed_certificate_reports_new_fingerprint() = runTest(dispatcher) {
        val repo = FakeConnectionRepository()
        val vm = createViewModel(repository = repo, tester = PinningFakeTester(serverSha256 = NEW_SERVER_PIN))
        val saved = ComfyUIConnection(
            id = SAVED_ID,
            name = "PC",
            hostname = "10.0.0.9",
            useHttps = true,
            acceptSelfSigned = true,
            tlsCertSha256 = SERVER_PIN,
        )

        vm.onReviewCertificate(saved)
        dispatcher.scheduler.advanceUntilIdle()

        val failure = vm.uiState.value.step
        assertIs<OnboardingStep.Failure>(failure)
        assertEquals(ConnectionFailureCause.CertificateChanged, failure.cause)
        assertEquals(NEW_SERVER_PIN, failure.presentedSha256)
        assertTrue(repo.saved.isEmpty())

        vm.onTrustCertificate()
        dispatcher.scheduler.advanceUntilIdle()

        assertIs<OnboardingStep.Success>(vm.uiState.value.step)
        assertEquals(SAVED_ID, repo.saved.single().id)
        assertEquals(NEW_SERVER_PIN, repo.saved.single().tlsCertSha256)
    }

    @Test
    fun review_of_saved_connection_with_matching_pin_succeeds() = runTest(dispatcher) {
        val repo = FakeConnectionRepository()
        val vm = createViewModel(repository = repo, tester = PinningFakeTester(serverSha256 = SERVER_PIN))
        val saved = ComfyUIConnection(
            id = SAVED_ID,
            name = "PC",
            hostname = "10.0.0.9",
            useHttps = true,
            acceptSelfSigned = true,
            tlsCertSha256 = SERVER_PIN,
        )

        vm.onReviewCertificate(saved)
        dispatcher.scheduler.advanceUntilIdle()

        assertIs<OnboardingStep.Success>(vm.uiState.value.step)
        assertEquals(SERVER_PIN, repo.saved.single().tlsCertSha256)
    }

    @Test
    fun trust_certificate_is_ignored_for_other_failures() = runTest(dispatcher) {
        val tester = FakeTester(ConnectionTestResult.Failure(ConnectionFailureCause.Tls))
        val repo = FakeConnectionRepository()
        val vm = createViewModel(repository = repo, tester = tester)
        vm.onManualSubmit("PC", "10.0.0.9", DEFAULT_PORT, useHttps = true, acceptSelfSigned = true)
        dispatcher.scheduler.advanceUntilIdle()

        tester.result = ConnectionTestResult.Success(null)
        vm.onTrustCertificate()
        dispatcher.scheduler.advanceUntilIdle()

        val step = vm.uiState.value.step
        assertIs<OnboardingStep.Failure>(step)
        assertEquals(ConnectionFailureCause.Tls, step.cause)
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun choose_method_resets_step() = runTest(dispatcher) {
        val vm = createViewModel()
        vm.onStartScan()
        dispatcher.scheduler.advanceUntilIdle()

        vm.onChooseMethod()

        assertIs<OnboardingStep.ChooseMethod>(vm.uiState.value.step)
    }

    private fun createViewModel(
        discovery: ServerDiscoveryRepository = FakeDiscovery(emptyList()),
        tester: ComfyUIConnectionTester = FakeTester(ConnectionTestResult.Success(null)),
        repository: ComfyUIConnectionRepository = FakeConnectionRepository(),
        lanScanSupported: Boolean = true,
    ) = ConnectionOnboardingViewModel(
        scanForServers = ScanForServersUseCase(discovery),
        connectionTester = tester,
        parseConnectionUrl = ParseConnectionUrlUseCase(),
        saveConnection = SaveComfyUIConnectionUseCase(repository),
        activateConnection = ActivateComfyUIConnectionUseCase(repository),
        lanScanSupported = lanScanSupported,
    )

    private fun systemStats() = SystemStats(
        gpuName = "RTX 4090",
        gpuType = "cuda",
        vramTotalMB = 24_000,
        vramFreeMB = 20_000,
        ramTotalMB = 64_000,
        ramFreeMB = 40_000,
        os = "Linux",
        comfyuiVersion = "0.3.0",
        pytorchVersion = "2.4.0",
    )

    private companion object {
        const val DEFAULT_PORT = 8188
        const val HTTPS_PORT = 443
        const val SAVED_ID = 7L
        val SERVER_PIN = "a".repeat(64)
        val NEW_SERVER_PIN = "b".repeat(64)
    }
}

/**
 * Answers like the pinned tester against a server presenting [serverSha256]: an unpinned
 * self-signed HTTPS connection is unconfirmed, a different pin is a changed certificate.
 */
private class PinningFakeTester(private val serverSha256: String) : ComfyUIConnectionTester {
    val tested = mutableListOf<ComfyUIConnection>()

    override suspend fun test(connection: ComfyUIConnection): ConnectionTestResult {
        tested += connection
        val pin = connection.tlsCertSha256
        return when {
            !(connection.useHttps && connection.acceptSelfSigned) -> ConnectionTestResult.Success(null)
            pin == null -> certificateFailure(ConnectionFailureCause.CertificateUnconfirmed)
            pin != serverSha256 -> certificateFailure(ConnectionFailureCause.CertificateChanged)
            else -> ConnectionTestResult.Success(null)
        }
    }

    private fun certificateFailure(cause: ConnectionFailureCause) =
        ConnectionTestResult.Failure(cause, presentedSha256 = serverSha256)
}

private class FakeDiscovery(private val servers: List<DiscoveredServer>) : ServerDiscoveryRepository {
    override fun scanForServers(): Flow<List<DiscoveredServer>> = flow {
        emit(emptyList())
        if (servers.isNotEmpty()) emit(servers)
    }
}

private class FakeTester(var result: ConnectionTestResult) : ComfyUIConnectionTester {
    val tested = mutableListOf<ComfyUIConnection>()

    override suspend fun test(connection: ComfyUIConnection): ConnectionTestResult {
        tested += connection
        return result
    }
}

private class FakeConnectionRepository : ComfyUIConnectionRepository {
    val saved = mutableListOf<ComfyUIConnection>()
    var activatedId: Long? = null
    private var nextId = 1L
    private val connections = MutableStateFlow<List<ComfyUIConnection>>(emptyList())
    private val active = MutableStateFlow<ComfyUIConnection?>(null)

    override fun observeConnections(): Flow<List<ComfyUIConnection>> = connections
    override fun observeActiveConnection(): Flow<ComfyUIConnection?> = active
    override suspend fun getActiveConnection(): ComfyUIConnection? = active.value

    override suspend fun saveConnection(connection: ComfyUIConnection): Long {
        val id = if (connection.id == 0L) nextId++ else connection.id
        saved.add(connection.copy(id = id))
        return id
    }

    override suspend fun deleteConnection(id: Long) { saved.removeAll { it.id == id } }
    override suspend fun activateConnection(id: Long) { activatedId = id }
    override suspend fun testConnection(connection: ComfyUIConnection): Boolean = true
    override suspend fun updateTestResult(id: Long, success: Boolean) { /* no-op */ }
}
