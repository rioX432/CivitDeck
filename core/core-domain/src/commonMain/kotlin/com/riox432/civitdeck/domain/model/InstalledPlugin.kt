package com.riox432.civitdeck.domain.model

/**
 * Domain model representing a plugin that has been installed in the app.
 * This is a pure domain type — it does not depend on core-plugin types.
 */
data class InstalledPlugin(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val pluginType: InstalledPluginType,
    val capabilities: List<String>,
    val minAppVersion: String,
    val state: InstalledPluginState,
    val configJson: String = "{}",
    val installedAt: Long = 0,
    val updatedAt: Long = 0,
)

enum class InstalledPluginState {
    INSTALLED,
    ACTIVE,
    INACTIVE,
    ERROR,
}

enum class InstalledPluginType {
    WORKFLOW_ENGINE,
    EXPORT_FORMAT,
    THEME,
}
