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

    suspend fun putCache(key: String, json: String) {
        dao.insert(
            CachedApiResponseEntity(
                cacheKey = key,
                responseJson = json,
                cachedAt = currentTimeMillis(),
            ),
        )
        dao.deleteExpired(currentTimeMillis() - DEFAULT_TTL_MILLIS)
    }

    suspend fun clearExpired() {
        dao.deleteExpired(currentTimeMillis() - DEFAULT_TTL_MILLIS)
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }

    companion object {
        const val DEFAULT_TTL_MILLIS = 15L * 60L * 1000L // 15 minutes
    }
}
