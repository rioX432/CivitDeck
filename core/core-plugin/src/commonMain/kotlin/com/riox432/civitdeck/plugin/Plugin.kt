package com.riox432.civitdeck.plugin

import com.riox432.civitdeck.plugin.model.PluginManifest
import com.riox432.civitdeck.plugin.model.PluginState

/**
 * Core plugin interface that all plugin types must implement.
 */
interface Plugin {
    val manifest: PluginManifest
    val state: PluginState

    suspend fun initialize()
    suspend fun activate()
    suspend fun deactivate()
    suspend fun destroy()
}
