package com.riox432.civitdeck.feature.comfyui.plugin

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.repository.ComfyUIConnectionRepository
import com.riox432.civitdeck.plugin.WorkflowEnginePlugin
import com.riox432.civitdeck.plugin.WorkflowEngineStatus
import com.riox432.civitdeck.plugin.capability.WorkflowCapability
import com.riox432.civitdeck.plugin.model.PluginManifest
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType
import kotlinx.coroutines.flow.first

/**
 * Adapts the existing ComfyUI integration to the WorkflowEnginePlugin interface.
 */
class ComfyUIWorkflowPlugin(
    private val connectionRepository: ComfyUIConnectionRepository,
) : WorkflowEnginePlugin {

    override val capabilities: Set<WorkflowCapability> = setOf(
        WorkflowCapability.IMAGE_GENERATION,
        WorkflowCapability.QUEUE_MANAGEMENT,
        WorkflowCapability.WORKFLOW_IMPORT,
    )

    override val manifest = PluginManifest(
        id = PLUGIN_ID,
        name = "ComfyUI",
        version = "1.0.0",
        author = "CivitDeck",
        description = "Connect to a ComfyUI server for image generation and workflow management",
        pluginType = PluginType.WORKFLOW_ENGINE,
        capabilities = capabilities.map { it.name },
    )

    override var state: PluginState = PluginState.INSTALLED
        private set

    private var cachedConnection: ComfyUIConnection? = null

    override val isConnected: Boolean
        get() = state == PluginState.ACTIVE && cachedConnection?.lastTestSuccess == true

    override suspend fun initialize() {
        refreshActiveConnection()
    }

    override suspend fun activate() {
        state = PluginState.ACTIVE
    }

    override suspend fun deactivate() {
        state = PluginState.INACTIVE
    }

    override suspend fun destroy() {
        state = PluginState.INSTALLED
        cachedConnection = null
    }

    override suspend fun connect(): Result<Unit> = runCatching {
        refreshActiveConnection()
        val connection = cachedConnection ?: error("No active ComfyUI connection")
        val success = connectionRepository.testConnection(connection)
        connectionRepository.updateTestResult(connection.id, success)
        if (!success) error("Connection test failed")
        state = PluginState.ACTIVE
    }

    override suspend fun disconnect() {
        state = PluginState.INACTIVE
    }

    override suspend fun getStatus(): WorkflowEngineStatus {
        refreshActiveConnection()
        val connection = cachedConnection
        return WorkflowEngineStatus(
            isConnected = isConnected,
            serverName = connection?.name ?: "Not configured",
            serverUrl = connection?.baseUrl ?: "",
            capabilities = capabilities,
        )
    }

    private suspend fun refreshActiveConnection() {
        cachedConnection = connectionRepository.observeActiveConnection().first()
    }

    companion object {
        const val PLUGIN_ID = "workflow.comfyui"
    }
}
