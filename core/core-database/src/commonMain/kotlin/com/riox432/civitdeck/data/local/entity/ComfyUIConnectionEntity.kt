package com.riox432.civitdeck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comfyui_connections")
data class ComfyUIConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hostname: String,
    val port: Int = DEFAULT_PORT,
    val isActive: Boolean = false,
    val lastTestedAt: Long? = null,
    val lastTestSuccess: Boolean? = null,
    val createdAt: Long,
) {
    companion object {
        const val DEFAULT_PORT = 8188
    }
}
