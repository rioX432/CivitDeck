package com.riox432.civitdeck.feature.externalserver.plugin

import com.riox432.civitdeck.domain.model.ExternalServerConfig
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerConfigRepository
import com.riox432.civitdeck.feature.externalserver.domain.repository.ExternalServerImagesRepository
import com.riox432.civitdeck.plugin.WorkflowEnginePlugin
import com.riox432.civitdeck.plugin.WorkflowEngineStatus
import com.riox432.civitdeck.plugin.capability.WorkflowCapability
import com.riox432.civitdeck.plugin.model.PluginManifest
import com.riox432.civitdeck.plugin.model.PluginState
import com.riox432.civitdeck.plugin.model.PluginType
import com.riox432.civitdeck.domain.util.suspendRunCatching
import kotlinx.coroutines.flow.first

/**
 * Adapts the existing ExternalServer integration to the WorkflowEnginePlugin interface.
 */
class ExternalServerWorkflowPlugin(
    private val configRepository: ExternalServerConfigRepository,
    private val imagesRepository: ExternalServerImagesRepository,
) : WorkflowEnginePlugin {

    override val capabilities: Set<WorkflowCapability> = setOf(
        WorkflowCapability.IMAGE_GENERATION,
        WorkflowCapability.IMAGE_BROWSING,
    )

    override val manifest = PluginManifest(
        id = PLUGIN_ID,
        name = "External Server",
        version = "1.0.0",
        author = "CivitDeck",
        description = "Connect to an external image server for browsing and generation",
        pluginType = PluginType.WORKFLOW_ENGINE,
        capabilities = capabilities.map { it.name },
    )

    override var state: PluginState = PluginState.INSTALLED
        private set

    private var cachedConfig: ExternalServerConfig? = null

    override val isConnected: Boolean
        get() = state == PluginState.ACTIVE && cachedConfig?.lastTestSuccess == true

    override suspend fun initialize() {
        refreshActiveConfig()
    }

    override suspend fun activate() {
        state = PluginState.ACTIVE
    }

    override suspend fun deactivate() {
        state = PluginState.INACTIVE
    }

    override suspend fun destroy() {
        state = PluginState.INSTALLED
        cachedConfig = null
    }

    override suspend fun connect(): Result<Unit> = suspendRunCatching {
        refreshActiveConfig()
        val config = cachedConfig ?: error("No active external server configuration")
        val success = imagesRepository.testConnection()
        configRepository.updateTestResult(config.id, success)
        if (!success) error("Connection test failed")
        state = PluginState.ACTIVE
    }

    override suspend fun disconnect() {
        state = PluginState.INACTIVE
    }

    override suspend fun getStatus(): WorkflowEngineStatus {
        refreshActiveConfig()
        val config = cachedConfig
        return WorkflowEngineStatus(
            isConnected = isConnected,
            serverName = config?.name ?: "Not configured",
            serverUrl = config?.baseUrl ?: "",
            capabilities = capabilities,
        )
    }

    private suspend fun refreshActiveConfig() {
        cachedConfig = configRepository.observeActiveConfig().first()
    }

    companion object {
        const val PLUGIN_ID = "workflow.external-server"
    }
}
