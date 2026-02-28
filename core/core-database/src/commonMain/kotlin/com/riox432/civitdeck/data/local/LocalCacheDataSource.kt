package com.riox432.civitdeck.data.local

import com.riox432.civitdeck.data.local.dao.CachedApiResponseDao
import com.riox432.civitdeck.data.local.entity.CachedApiResponseEntity

class LocalCacheDataSource(
    private val dao: CachedApiResponseDao,
) {
    suspend fun getCached(key: String, ttlMillis: Long = DEFAULT_TTL_MILLIS): String? {
        val cached = dao.getByKey(key) ?: return null
        val now = currentTimeMillis()
        return if (now - cached.cachedAt < ttlMillis) {
            cached.responseJson
        } else {
            null
        }
    }

    /**
     * Get cached data regardless of TTL expiry (for offline mode).
     * Returns null only if the key has never been cached.
     */
    suspend fun getCachedIgnoringTtl(key: String): String? {
        return dao.getByKey(key)?.responseJson
    }

    suspend fun putCache(key: String, json: String) {
        val existing = dao.getByKey(key)
        dao.insert(
            CachedApiResponseEntity(
                cacheKey = key,
                responseJson = json,
                cachedAt = currentTimeMillis(),
                isOfflinePinned = existing?.isOfflinePinned ?: false,
            ),
        )
        dao.deleteExpired(currentTimeMillis() - OFFLINE_TTL_MILLIS)
    }

    suspend fun pinForOffline(key: String) {
        dao.setPinned(key, true)
    }

    suspend fun unpinForOffline(key: String) {
        dao.setPinned(key, false)
    }

    suspend fun clearExpired() {
        dao.deleteExpired(currentTimeMillis() - OFFLINE_TTL_MILLIS)
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }

    suspend fun getCacheSizeBytes(): Long {
        return dao.getTotalCacheSizeBytes() ?: 0L
    }

    suspend fun getEntryCount(): Int {
        return dao.getEntryCount()
    }

    /**
     * Evict oldest unpinned entries to keep cache under the given byte limit.
     */
    suspend fun evictToSize(maxBytes: Long) {
        var currentSize = getCacheSizeBytes()
        while (currentSize > maxBytes) {
            val countBefore = dao.getEntryCount()
            dao.deleteOldestUnpinned(EVICTION_BATCH_SIZE)
            val countAfter = dao.getEntryCount()
            if (countAfter >= countBefore) break // No more unpinned entries to delete
            currentSize = getCacheSizeBytes()
        }
    }

    companion object {
        const val DEFAULT_TTL_MILLIS = 15L * 60L * 1000L // 15 minutes
        const val OFFLINE_TTL_MILLIS = 7L * 24L * 60L * 60L * 1000L // 7 days
        private const val EVICTION_BATCH_SIZE = 50
    }
}
