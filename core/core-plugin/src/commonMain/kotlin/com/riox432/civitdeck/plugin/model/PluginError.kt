package com.riox432.civitdeck.plugin.model

sealed class PluginError(override val message: String) : Exception(message) {
    class InitializationFailed(message: String) : PluginError(message)
    class AlreadyRegistered(pluginId: String) : PluginError("Plugin '$pluginId' is already registered")
    class NotFound(pluginId: String) : PluginError("Plugin '$pluginId' not found")
    class IncompatibleVersion(message: String) : PluginError(message)
    class ActivationFailed(message: String) : PluginError(message)
}
