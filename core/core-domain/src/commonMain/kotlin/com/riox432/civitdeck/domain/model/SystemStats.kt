package com.riox432.civitdeck.domain.model

/**
 * Hardware and system information from a connected ComfyUI server.
 * Memory values are in megabytes for display convenience.
 */
data class SystemStats(
    val gpuName: String,
    val gpuType: String,
    val vramTotalMB: Long,
    val vramFreeMB: Long,
    val ramTotalMB: Long,
    val ramFreeMB: Long,
    val os: String,
    val comfyuiVersion: String?,
    val pytorchVersion: String?,
)
