package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val pluginType: String,
    val capabilities: String,
    val minAppVersion: String,
    val state: String,
    val configJson: String = "{}",
    val installedAt: Long,
    val updatedAt: Long,
)
