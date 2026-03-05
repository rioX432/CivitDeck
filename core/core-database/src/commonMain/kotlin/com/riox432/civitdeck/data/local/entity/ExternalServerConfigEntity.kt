package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "external_server_configs")
data class ExternalServerConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val baseUrl: String,
    val apiKey: String = "",
    val isActive: Boolean = false,
    val lastTestedAt: Long? = null,
    val lastTestSuccess: Boolean? = null,
    val createdAt: Long,
)
