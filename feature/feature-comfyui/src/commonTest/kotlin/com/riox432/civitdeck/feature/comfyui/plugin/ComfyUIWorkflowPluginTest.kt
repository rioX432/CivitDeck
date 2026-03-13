package com.riox432.civitdeck.feature.comfyui.plugin

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
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

class ComfyUIWorkflowPluginTest {

    private val activeConnection = ComfyUIConnection(
        id = 1L,
        name = "Local ComfyUI",
        hostname = "127.0.0.1",
        port = 8188,
        isActive = true,
        lastTestedAt = 1000L,
        lastTestSuccess = true,
    )

    private fun createPlugin(
        connection: ComfyUIConnection? = activeConnection,
        testResult: Boolean = true,
    ): ComfyUIWorkflowPlugin {
        val repo = FakeComfyUIConnectionRepository(connection, testResult)
        return ComfyUIWorkflowPlugin(repo)
    }

    @Test
    fun manifestHasCorrectValues() {
        val plugin = createPlugin()
        assertEquals(ComfyUIWorkflowPlugin.PLUGIN_ID, plugin.manifest.id)
        assertEquals("ComfyUI", plugin.manifest.name)
        assertEquals(PluginType.WORKFLOW_ENGINE, plugin.manifest.pluginType)
    }

    @Test
    fun capabilitiesIncludeExpectedValues() {
        val plugin = createPlugin()
        assertTrue(WorkflowCapability.IMAGE_GENERATION in plugin.capabilities)
        assertTrue(WorkflowCapability.QUEUE_MANAGEMENT in plugin.capabilities)
        assertTrue(WorkflowCapability.WORKFLOW_IMPORT in plugin.capabilities)
        assertFalse(WorkflowCapability.IMAGE_BROWSING in plugin.capabilities)
    }

    @Test
    fun initialStateIsInstalled() {
        val plugin = createPlugin()
        assertEquals(PluginState.INSTALLED, plugin.state)
    }

    @Test
    fun connectSucceeds() = runTest {
        val plugin = createPlugin(testResult = true)
        val result = plugin.connect()
        assertTrue(result.isSuccess)
        assertEquals(PluginState.ACTIVE, plugin.state)
    }

    @Test
    fun connectFailsWhenNoConnection() = runTest {
        val plugin = createPlugin(connection = null)
        val result = plugin.connect()
        assertTrue(result.isFailure)
    }

    @Test
    fun connectFailsWhenTestFails() = runTest {
        val plugin = createPlugin(testResult = false)
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
    fun getStatusReturnsConnectionInfo() = runTest {
        val plugin = createPlugin()
        plugin.connect()
        val status = plugin.getStatus()
        assertTrue(status.isConnected)
        assertEquals("Local ComfyUI", status.serverName)
        assertEquals("http://127.0.0.1:8188", status.serverUrl)
    }

    @Test
    fun getStatusWhenNotConnected() = runTest {
        val plugin = createPlugin(connection = null)
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

private class FakeComfyUIConnectionRepository(
    private val activeConnection: ComfyUIConnection?,
    private val testResult: Boolean,
) : ComfyUIConnectionRepository {

    private val activeFlow = MutableStateFlow(activeConnection)

    override fun observeConnections(): Flow<List<ComfyUIConnection>> =
        MutableStateFlow(listOfNotNull(activeConnection))

    override fun observeActiveConnection(): Flow<ComfyUIConnection?> = activeFlow

    override suspend fun getActiveConnection(): ComfyUIConnection? = activeFlow.value

    override suspend fun saveConnection(connection: ComfyUIConnection): Long = connection.id

    override suspend fun deleteConnection(id: Long) {}

    override suspend fun activateConnection(id: Long) {}

    override suspend fun testConnection(connection: ComfyUIConnection): Boolean = testResult

    override suspend fun updateTestResult(id: Long, success: Boolean) {
        val current = activeFlow.value ?: return
        activeFlow.value = current.copy(lastTestSuccess = success)
    }
}
