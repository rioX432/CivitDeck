package com.riox432.civitdeck.di

import com.riox432.civitdeck.data.export.KohyaZipExportPlugin
import com.riox432.civitdeck.plugin.PluginRegistry
import org.koin.mp.KoinPlatform

/**
 * Registers built-in export format plugins with the PluginRegistry.
 * Should be called once after Koin initialization.
 */
fun registerExportPlugins() {
    val koin = KoinPlatform.getKoin()
    val registry: PluginRegistry = koin.get()
    val kohyaZip: KohyaZipExportPlugin = koin.get()
    registry.register(kohyaZip)
}
