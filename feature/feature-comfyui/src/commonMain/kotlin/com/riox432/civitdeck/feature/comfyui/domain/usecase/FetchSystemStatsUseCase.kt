package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.data.api.comfyui.SystemStatsResponse
import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.SystemStats
import com.riox432.civitdeck.feature.comfyui.data.ComfyUIApiProvider
import com.riox432.civitdeck.util.Logger

private const val TAG = "FetchSystemStats"
private const val BYTES_PER_MB = 1_048_576L

/**
 * Fetches hardware and system information from a ComfyUI server.
 * Returns null if the server does not support /system_stats (older versions)
 * or if the request fails for any reason.
 */
class FetchSystemStatsUseCase(private val provider: ComfyUIApiProvider) {

    /** Stats of the active connection; null when no connection is active. */
    suspend operator fun invoke(): SystemStats? = fetchStatsOrNull { provider.forActive().api }

    /** Stats of [connection], over its own URL and pinned certificate. */
    suspend operator fun invoke(connection: ComfyUIConnection): SystemStats? =
        fetchStatsOrNull { provider.forConnection(connection).api }
}

/** Fetches stats over [api], for callers that already hold a client bound to one server. */
internal suspend fun fetchSystemStats(api: ComfyUIApi): SystemStats? = fetchStatsOrNull { api }

private suspend fun fetchStatsOrNull(resolveApi: suspend () -> ComfyUIApi): SystemStats? = try {
    mapToSystemStats(resolveApi().getSystemStats())
} catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
    Logger.w(TAG, "Failed to fetch system stats: ${e.message}")
    null
}

private fun mapToSystemStats(response: SystemStatsResponse): SystemStats {
    val device = response.devices.firstOrNull()
    return SystemStats(
        gpuName = device?.name ?: "Unknown",
        gpuType = device?.type ?: "Unknown",
        vramTotalMB = (device?.vramTotal ?: 0L) / BYTES_PER_MB,
        vramFreeMB = (device?.vramFree ?: 0L) / BYTES_PER_MB,
        ramTotalMB = response.system.ramTotal / BYTES_PER_MB,
        ramFreeMB = response.system.ramFree / BYTES_PER_MB,
        os = response.system.os,
        comfyuiVersion = response.system.comfyuiVersion,
        pytorchVersion = response.system.pytorchVersion,
    )
}
