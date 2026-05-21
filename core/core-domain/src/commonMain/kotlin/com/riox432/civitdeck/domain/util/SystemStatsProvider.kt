package com.riox432.civitdeck.domain.util

import com.riox432.civitdeck.domain.model.SystemStats

/**
 * Functional interface for fetching system stats from the connected
 * ComfyUI server. Returns null when no server is connected or the
 * endpoint is unavailable.
 *
 * This abstraction lives in core-domain so feature modules can
 * consume system stats without depending on feature-comfyui.
 */
fun interface SystemStatsProvider {
    suspend fun fetch(): SystemStats?
}
