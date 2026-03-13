package com.riox432.civitdeck.feature.externalserver.plugin

import com.riox432.civitdeck.domain.model.ExternalServerConfig
import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationChoice
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJob
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationOption
import com.riox432.civitdeck.feature.externalserver.domain.model.PaginatedImagesResponse
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerCapabilities
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerConfigRepository
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerImagesRepository
import com.riox432.civitdeck.plugin.capability.WorkflowCapability
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalServerWorkflowPluginTest {

    private val activeConfig = ExternalServerConfig(
        id = 1L,
        name = "Test Server",
        baseUrl = "http://localhost:8080",
        apiKey = "key",
        isActive = true,
        lastTestedAt = 1000L,
        lastTestSuccess = true,
        createdAt = 500L,
    )

    private fun createPlugin(
        config: ExternalServerConfig? = activeConfig,
        testConnectionResult: Boolean = true,
    ): ExternalServerWorkflowPlugin {
        val configRepo = FakeExternalServerConfigRepository(config)
        val imagesRepo = FakeExternalServerImagesRepository(testConnectionResult)
        return ExternalServerWorkflowPlugin(configRepo, imagesRepo)
    }

    @Test
    fun manifestHasCorrectValues() {
        val plugin = createPlugin()
        assertEquals(ExternalServerWorkflowPlugin.PLUGIN_ID, plugin.manifest.id)
        assertEquals("External Server", plugin.manifest.name)
        assertEquals(PluginType.WORKFLOW_ENGINE, plugin.manifest.pluginType)
    }

    @Test
    fun capabilitiesIncludeImageGenerationAndBrowsing() {
        val plugin = createPlugin()
        assertTrue(WorkflowCapability.IMAGE_GENERATION in plugin.capabilities)
        assertTrue(WorkflowCapability.IMAGE_BROWSING in plugin.capabilities)
        assertFalse(WorkflowCapability.QUEUE_MANAGEMENT in plugin.capabilities)
    }

    @Test
    fun initialStateIsInstalled() {
        val plugin = createPlugin()
        assertEquals(PluginState.INSTALLED, plugin.state)
    }

    @Test
    fun connectSucceeds() = runTest {
        val plugin = createPlugin(testConnectionResult = true)
        val result = plugin.connect()
        assertTrue(result.isSuccess)
        assertEquals(PluginState.ACTIVE, plugin.state)
    }

    @Test
    fun connectFailsWhenNoConfig() = runTest {
        val plugin = createPlugin(config = null)
        val result = plugin.connect()
        assertTrue(result.isFailure)
    }

    @Test
    fun connectFailsWhenTestFails() = runTest {
        val plugin = createPlugin(testConnectionResult = false)
        val result = plugin.connect()
        assertTrue(result.isFailure)
    }

    @Test
    fun disconnectSetsInactive() = runTest {
        val plugin = createPlugin()
        plugin.connect()
        plugin.disconnect()
        assertEquals(PluginState.INACTIVE, plugin.state)
    }

    @Test
    fun getStatusReturnsServerInfo() = runTest {
        val plugin = createPlugin()
        plugin.connect()
        val status = plugin.getStatus()
        assertTrue(status.isConnected)
        assertEquals("Test Server", status.serverName)
        assertEquals("http://localhost:8080", status.serverUrl)
    }

    @Test
    fun getStatusWhenNotConnected() = runTest {
        val plugin = createPlugin(config = null)
        val status = plugin.getStatus()
        assertFalse(status.isConnected)
        assertEquals("Not configured", status.serverName)
    }

    @Test
    fun activateAndDeactivate() = runTest {
        val plugin = createPlugin()
        plugin.activate()
        assertEquals(PluginState.ACTIVE, plugin.state)
        plugin.deactivate()
        assertEquals(PluginState.INACTIVE, plugin.state)
    }
}

private class FakeExternalServerConfigRepository(
    private val activeConfig: ExternalServerConfig?,
) : ExternalServerConfigRepository {

    private val activeFlow = MutableStateFlow(activeConfig)

    override fun observeConfigs(): Flow<List<ExternalServerConfig>> =
        MutableStateFlow(listOfNotNull(activeConfig))

    override fun observeActiveConfig(): Flow<ExternalServerConfig?> = activeFlow

    override suspend fun saveConfig(config: ExternalServerConfig): Long = config.id

    override suspend fun deleteConfig(id: Long) {}

    override suspend fun activateConfig(id: Long) {}

    override suspend fun updateTestResult(id: Long, success: Boolean) {
        val current = activeFlow.value ?: return
        activeFlow.value = current.copy(lastTestSuccess = success)
    }
}

private class FakeExternalServerImagesRepository(
    private val testResult: Boolean,
) : ExternalServerImagesRepository {
    override suspend fun getCapabilities() = ServerCapabilities()
    override suspend fun getImages(
        page: Int,
        perPage: Int,
        filters: ExternalServerImageFilters,
    ) = PaginatedImagesResponse(emptyList(), 0, 1, 20, 0)
    override suspend fun testConnection(): Boolean = testResult
    override suspend fun getGenerationOptions(): List<GenerationOption> = emptyList()
    override suspend fun getDependentChoices(endpoint: String): List<GenerationChoice> = emptyList()
    override suspend fun executeGeneration(params: Map<String, String>): GenerationJob =
        error("Not implemented")
    override suspend fun getGenerationStatus(jobId: String): GenerationJob =
        error("Not implemented")
}
