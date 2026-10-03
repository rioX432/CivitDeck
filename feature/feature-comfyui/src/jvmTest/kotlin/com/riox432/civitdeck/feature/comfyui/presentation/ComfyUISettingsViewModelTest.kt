package com.riox432.civitdeck.feature.comfyui.presentation

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ComfyUIConnectionStatus
import com.riox432.civitdeck.domain.model.DiscoveredServer
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.domain.repository.ServerDiscoveryRepository
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import com.riox432.civitdeck.feature.comfyui.data.NtfySubscriptionService
import com.riox432.civitdeck.feature.comfyui.data.repository.FakeComfyUIConnectionDao
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ActivateComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.DeleteComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.FetchSystemStatsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveActiveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ObserveComfyUIConnectionsUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ParseConnectionUrlUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.SaveComfyUIConnectionUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.ScanForServersUseCase
import com.riox432.civitdeck.feature.comfyui.domain.usecase.TestComfyUIConnectionUseCase
import com.riox432.civitdeck.testing.FakeGenerationNotificationService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [ComfyUISettingsViewModel] state transitions for scan / test / save.
 *
 * Lives in jvmTest because these use-case tests need `android.util.Log` unmocked
 * on the JVM target.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ComfyUISettingsViewModelTest {

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun activeConnection(id: Long = 1L) = ComfyUIConnection(
        id = id,
        name = "Local",
        hostname = "127.0.0.1",
        port = 8188,
    )

    private class FakeConnectionRepo(
        connections: List<ComfyUIConnection> = emptyList(),
        active: ComfyUIConnection? = null,
        private val testResult: Boolean = true,
    ) : ComfyUIConnectionRepository {
        val connectionsFlow = MutableStateFlow(connections)
        val activeFlow = MutableStateFlow(active)
        var savedConnection: ComfyUIConnection? = null
        var deletedId: Long? = null
        var activatedId: Long? = null
        var testedConnection: ComfyUIConnection? = null

        override fun observeConnections(): Flow<List<ComfyUIConnection>> = connectionsFlow
        override fun observeActiveConnection(): Flow<ComfyUIConnection?> = activeFlow
        override suspend fun getActiveConnection(): ComfyUIConnection? = activeFlow.value
        override suspend fun saveConnection(connection: ComfyUIConnection): Long {
            savedConnection = connection
            return 1L
        }
        override suspend fun deleteConnection(id: Long) {
            deletedId = id
        }
        override suspend fun activateConnection(id: Long) {
            activatedId = id
        }
        override suspend fun testConnection(connection: ComfyUIConnection): Boolean {
            testedConnection = connection
            return testResult
        }
        override suspend fun updateTestResult(id: Long, success: Boolean) = Unit
    }

    private class FakeServerDiscoveryRepo(
        private val servers: List<DiscoveredServer> = emptyList(),
        private val failWith: Throwable? = null,
    ) : ServerDiscoveryRepository {
        override fun scanForServers(): Flow<List<DiscoveredServer>> = flow {
            failWith?.let { throw it }
            emit(servers)
        }
    }

    private fun mockProvider(): ComfyUIApiProvider {
        // System stats endpoint returns an error so fetchSystemStats() yields null,
        // keeping the focus on test/save/scan state transitions.
        val engine = MockEngine { respondError(HttpStatusCode.NotFound) }
        return ComfyUIApiProvider(FakeComfyUIConnectionDao(), HttpClient(engine), Json { ignoreUnknownKeys = true })
    }

    private fun TestScope.ntfyService(): NtfySubscriptionService {
        val engine = MockEngine { respondError(HttpStatusCode.NotFound) }
        return NtfySubscriptionService(
            httpClient = HttpClient(engine),
            notificationService = FakeGenerationNotificationService(),
            scope = this,
        )
    }

    private fun TestScope.createViewModel(
        connectionRepo: FakeConnectionRepo,
        discoveryRepo: FakeServerDiscoveryRepo = FakeServerDiscoveryRepo(),
    ): ComfyUISettingsViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        return ComfyUISettingsViewModel(
            observeConnections = ObserveComfyUIConnectionsUseCase(connectionRepo),
            observeActive = ObserveActiveComfyUIConnectionUseCase(connectionRepo),
            saveConnection = SaveComfyUIConnectionUseCase(connectionRepo),
            deleteConnection = DeleteComfyUIConnectionUseCase(connectionRepo),
            activateConnection = ActivateComfyUIConnectionUseCase(connectionRepo),
            testConnection = TestComfyUIConnectionUseCase(connectionRepo),
            scanForServers = ScanForServersUseCase(discoveryRepo),
            fetchSystemStats = FetchSystemStatsUseCase(mockProvider()),
            ntfyService = ntfyService(),
            parseConnectionUrl = ParseConnectionUrlUseCase(),
        )
    }

    @Test
    fun save_connection_forwards_to_repository_and_closes_dialog() = runTest {
        val repo = FakeConnectionRepo()
        val vm = createViewModel(repo)
        vm.onShowAddDialog()

        vm.onSaveConnection(name = "Remote", hostname = "10.0.0.5", port = 8188)
        advanceUntilIdle()

        assertEquals("Remote", repo.savedConnection?.name)
        assertEquals("10.0.0.5", repo.savedConnection?.hostname)
        assertEquals(8188, repo.savedConnection?.port)
        assertEquals(false, repo.savedConnection?.useHttps)
        assertFalse(vm.uiState.value.showAddDialog)
    }

    @Test
    fun save_connection_parses_pasted_http_url() = runTest {
        val repo = FakeConnectionRepo()
        val vm = createViewModel(repo)

        vm.onSaveConnection(name = "PC", hostname = "http://10.0.0.5:8188/", port = 8188)
        advanceUntilIdle()

        assertEquals("PC", repo.savedConnection?.name)
        assertEquals("10.0.0.5", repo.savedConnection?.hostname)
        assertEquals(8188, repo.savedConnection?.port)
        assertEquals(false, repo.savedConnection?.useHttps)
    }

    @Test
    fun save_connection_pasted_https_url_without_port_uses_443() = runTest {
        val repo = FakeConnectionRepo()
        val vm = createViewModel(repo)

        vm.onSaveConnection(name = "PC", hostname = "https://pc.tailnet.ts.net", port = 8188)
        advanceUntilIdle()

        assertEquals("pc.tailnet.ts.net", repo.savedConnection?.hostname)
        assertEquals(443, repo.savedConnection?.port)
        assertEquals(true, repo.savedConnection?.useHttps)
    }

    @Test
    fun save_connection_port_in_host_text_overrides_port_field() = runTest {
        val repo = FakeConnectionRepo()
        val vm = createViewModel(repo)

        vm.onSaveConnection(name = "PC", hostname = "192.168.1.5:8189", port = 8188, useHttps = true)
        advanceUntilIdle()

        assertEquals("192.168.1.5", repo.savedConnection?.hostname)
        assertEquals(8189, repo.savedConnection?.port)
        // No scheme in the text, so the HTTPS toggle still applies.
        assertEquals(true, repo.savedConnection?.useHttps)
    }

    @Test
    fun save_connection_with_name_defaulted_to_pasted_url_uses_parsed_host_as_name() = runTest {
        val repo = FakeConnectionRepo()
        val vm = createViewModel(repo)
        val pasted = "http://10.0.0.5:8188/"

        // The dialogs pass the host text as the name when the name field is left blank.
        vm.onSaveConnection(name = pasted, hostname = pasted, port = 8188)
        advanceUntilIdle()

        assertEquals("10.0.0.5", repo.savedConnection?.name)
    }

    @Test
    fun save_connection_keeps_unparseable_host_as_entered() = runTest {
        val repo = FakeConnectionRepo()
        val vm = createViewModel(repo)

        vm.onSaveConnection(name = "PC", hostname = "host:abc", port = 8190, useHttps = true)
        advanceUntilIdle()

        assertEquals("host:abc", repo.savedConnection?.hostname)
        assertEquals(8190, repo.savedConnection?.port)
        assertEquals(true, repo.savedConnection?.useHttps)
        assertFalse(vm.uiState.value.showAddDialog)
    }

    @Test
    fun save_edited_connection_keeps_id_and_bare_host_settings() = runTest {
        val existing = ComfyUIConnection(id = 7L, name = "PC", hostname = "10.0.0.5", port = 8190, useHttps = true)
        val repo = FakeConnectionRepo(connections = listOf(existing))
        val vm = createViewModel(repo)
        vm.onEditConnection(existing)

        vm.onSaveConnection(name = "PC", hostname = "10.0.0.5", port = 8190, useHttps = true)
        advanceUntilIdle()

        assertEquals(existing, repo.savedConnection)
    }

    @Test
    fun selecting_discovered_server_saves_its_ip_and_port() = runTest {
        val repo = FakeConnectionRepo()
        val vm = createViewModel(repo)

        vm.onSelectDiscoveredServer(
            DiscoveredServer(hostname = "comfy", ip = "192.168.1.10", port = 8190, displayName = "comfy"),
        )
        advanceUntilIdle()

        assertEquals("comfy", repo.savedConnection?.name)
        assertEquals("192.168.1.10", repo.savedConnection?.hostname)
        assertEquals(8190, repo.savedConnection?.port)
        assertEquals(false, repo.savedConnection?.useHttps)
    }

    @Test
    fun test_connection_success_runs_test_and_keeps_no_error() = runTest {
        val active = activeConnection()
        val repo = FakeConnectionRepo(active = active, testResult = true)
        val vm = createViewModel(repo)
        // A subscriber is required for the combine-based uiState to populate activeConnection.
        val collectJob = backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.onTestConnection()
        advanceUntilIdle()

        // The active connection was tested; the success branch never sets an error.
        // (isTesting flips back to false only after the subsequent system-stats fetch,
        // which hops onto Ktor's own dispatcher and is therefore not asserted here.)
        assertEquals(active, repo.testedConnection)
        assertNull(vm.uiState.value.testError)
        collectJob.cancel()
    }

    @Test
    fun test_connection_failure_sets_error() = runTest {
        val active = activeConnection()
        val repo = FakeConnectionRepo(active = active, testResult = false)
        val vm = createViewModel(repo)
        val collectJob = backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.onTestConnection()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isTesting)
        assertEquals("Connection failed", vm.uiState.value.testError)
        collectJob.cancel()
    }

    @Test
    fun test_connection_noop_without_active_connection() = runTest {
        val repo = FakeConnectionRepo()
        val vm = createViewModel(repo)
        val collectJob = backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.onTestConnection()
        advanceUntilIdle()

        assertNull(repo.testedConnection)
        assertFalse(vm.uiState.value.isTesting)
        assertEquals(ComfyUIConnectionStatus.NotConfigured, vm.uiState.value.connectionStatus)
        collectJob.cancel()
    }

    @Test
    fun scan_lan_populates_discovered_servers_then_finishes() = runTest {
        val servers = listOf(
            DiscoveredServer(
                hostname = "comfy",
                ip = "192.168.1.10",
                port = 8188,
                displayName = "comfy",
            ),
        )
        val repo = FakeConnectionRepo()
        val vm = createViewModel(repo, discoveryRepo = FakeServerDiscoveryRepo(servers))
        // uiState is a combine(...).stateIn(WhileSubscribed) — needs an active
        // subscriber to recompute from _mutableState changes.
        val collectJob = backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.onScanLan()
        advanceUntilIdle()

        assertEquals(servers, vm.uiState.value.discoveredServers)
        assertFalse(vm.uiState.value.isScanning)
        assertNull(vm.uiState.value.scanError)
        collectJob.cancel()
    }

    @Test
    fun scan_lan_failure_surfaces_error_to_ui_state() = runTest {
        val repo = FakeConnectionRepo()
        val vm = createViewModel(
            repo,
            discoveryRepo = FakeServerDiscoveryRepo(failWith = IllegalStateException("network down")),
        )
        val collectJob = backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.onScanLan()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isScanning)
        assertEquals("network down", vm.uiState.value.scanError)
        assertTrue(vm.uiState.value.discoveredServers.isEmpty())
        collectJob.cancel()
    }
}
