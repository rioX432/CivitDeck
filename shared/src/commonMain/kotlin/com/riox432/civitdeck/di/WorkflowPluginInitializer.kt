package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.repository.PluginRepository
import com.riox432.civitdeck.feature.comfyui.plugin.ComfyUIWorkflowPlugin
import com.riox432.civitdeck.feature.externalserver.plugin.ExternalServerWorkflowPlugin
import com.riox432.civitdeck.plugin.PluginRegistry
import org.koin.mp.KoinPlatform

/**
 * Registers built-in workflow engine plugins with the PluginRegistry
 * and persists them to the database so they appear in the Plugin Management screen.
 * Should be called once after Koin initialization.
 */
suspend fun registerWorkflowPlugins() {
    val koin = KoinPlatform.getKoin()
    val registry: PluginRegistry = koin.get()
    val pluginRepository: PluginRepository = koin.get()
    val externalServer: ExternalServerWorkflowPlugin = koin.get()
    val comfyUI: ComfyUIWorkflowPlugin = koin.get()
    registry.register(externalServer)
    registry.register(comfyUI)
    persistBuiltInPlugin(pluginRepository, externalServer)
    persistBuiltInPlugin(pluginRepository, comfyUI)
}
