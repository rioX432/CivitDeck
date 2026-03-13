package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.export.KohyaZipExportPlugin
import com.riox432.civitdeck.domain.repository.PluginRepository
import com.riox432.civitdeck.plugin.PluginRegistry
import org.koin.mp.KoinPlatform

/**
 * Registers built-in export format plugins with the PluginRegistry
 * and persists them to the database so they appear in the Plugin Management screen.
 * Should be called once after Koin initialization.
 */
suspend fun registerExportPlugins() {
    val koin = KoinPlatform.getKoin()
    val registry: PluginRegistry = koin.get()
    val pluginRepository: PluginRepository = koin.get()
    val kohyaZip: KohyaZipExportPlugin = koin.get()
    registry.register(kohyaZip)
    persistBuiltInPlugin(pluginRepository, kohyaZip)
}
