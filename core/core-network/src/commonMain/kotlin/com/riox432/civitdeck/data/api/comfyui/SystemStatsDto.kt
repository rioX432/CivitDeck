package com.riox432.civitdeck.data.api.comfyui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from GET /system_stats.
 * Available in ComfyUI 0.1.0+.
 */
@Serializable
data class SystemStatsResponse(
    val system: SystemInfo,
    val devices: List<DeviceInfo>,
)

@Serializable
data class SystemInfo(
    @SerialName("os") val os: String,
    @SerialName("ram_total") val ramTotal: Long,
    @SerialName("ram_free") val ramFree: Long,
    @SerialName("comfyui_version") val comfyuiVersion: String? = null,
    @SerialName("python_version") val pythonVersion: String? = null,
    @SerialName("pytorch_version") val pytorchVersion: String? = null,
    @SerialName("embedded_python") val embeddedPython: Boolean? = null,
)

@Serializable
data class DeviceInfo(
    val name: String,
    val type: String,
    @SerialName("vram_total") val vramTotal: Long,
    @SerialName("vram_free") val vramFree: Long,
    val index: Int = 0,
)
