package com.riox432.civitdeck.plugin.model

data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val pluginType: PluginType,
    val capabilities: List<String>,
    val minAppVersion: String = "",
)
