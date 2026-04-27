package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.domain.repository.ComfyUIGenerationRepository
import com.riox432.civitdeck.util.Logger

/**
 * Fetches the full /object_info response from the ComfyUI server, caching it locally with TTL.
 * This provides node type schemas needed for dynamic parameter extraction (dropdown options, ranges).
 */
class FetchObjectInfoUseCase(
    private val repository: ComfyUIGenerationRepository,
    private val cache: LocalCacheDataSource,
) {
    /**
     * Returns the /object_info JSON string, preferring cached data within TTL.
     * Falls back to stale cache on network error. Returns null if no data is available.
     */
    suspend operator fun invoke(forceRefresh: Boolean = false): String? {
        if (!forceRefresh) {
            val cached = cache.getCached(CACHE_KEY, OBJECT_INFO_TTL_MILLIS)
            if (cached != null) return cached
        }

        return try {
            val fresh = repository.fetchObjectInfo()
            cache.putCache(CACHE_KEY, fresh)
            fresh
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.w(TAG, "Failed to fetch object_info, falling back to cache: ${e.message}")
            // Fall back to stale cached data for offline support
            cache.getCachedIgnoringTtl(CACHE_KEY)
        }
    }

    companion object {
        private const val TAG = "FetchObjectInfo"
        private const val CACHE_KEY = "comfyui_object_info"

        // Cache TTL: 30 minutes (object_info changes rarely during a session)
        private const val OBJECT_INFO_TTL_MILLIS = 30L * 60L * 1000L
    }
}
