package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.data.local.dao.CachedApiResponseDao
import com.riox432.civitdeck.data.local.entity.CachedApiResponseEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [CacheRepositoryImpl] driven through a real [LocalCacheDataSource] backed by a
 * fake DAO. Covers cache-info aggregation, clear-all, and eviction down to a byte budget
 * (oldest unpinned first, pinned entries retained).
 */
class CacheRepositoryImplTest {

    private class FakeDao : CachedApiResponseDao {
        val rows = mutableListOf<CachedApiResponseEntity>()

        override suspend fun getByKey(key: String): CachedApiResponseEntity? =
            rows.firstOrNull { it.cacheKey == key }

        override suspend fun insert(entity: CachedApiResponseEntity) {
            rows.removeAll { it.cacheKey == entity.cacheKey }
            rows.add(entity)
        }

        override suspend fun deleteByKey(key: String): Int {
            val before = rows.size
            rows.removeAll { it.cacheKey == key }
            return before - rows.size
        }

        override suspend fun deleteExpired(expiryTime: Long): Int {
            val before = rows.size
            rows.removeAll { it.cachedAt < expiryTime && !it.isOfflinePinned }
            return before - rows.size
        }

        override suspend fun deleteAll(): Int {
            val count = rows.size
            rows.clear()
            return count
        }

        override suspend fun setPinned(key: String, pinned: Boolean): Int {
            val idx = rows.indexOfFirst { it.cacheKey == key }
            if (idx < 0) return 0
            rows[idx] = rows[idx].copy(isOfflinePinned = pinned)
            return 1
        }

        override suspend fun getTotalCacheSizeBytes(): Long? =
            if (rows.isEmpty()) null else rows.sumOf { it.responseJson.length.toLong() }

        override suspend fun getEntryCount(): Int = rows.size

        override suspend fun deleteOldestUnpinned(count: Int): Int {
            val targets = rows.filter { !it.isOfflinePinned }
                .sortedBy { it.cachedAt }
                .take(count)
            rows.removeAll(targets)
            return targets.size
        }
    }

    private fun row(key: String, json: String, cachedAt: Long, pinned: Boolean = false) =
        CachedApiResponseEntity(cacheKey = key, responseJson = json, cachedAt = cachedAt, isOfflinePinned = pinned)

    @Test
    fun getCacheInfo_aggregates_size_and_count() = runTest {
        val dao = FakeDao()
        dao.rows.add(row("a", "12345", 1L))
        dao.rows.add(row("b", "678", 2L))
        val repo = CacheRepositoryImpl(LocalCacheDataSource(dao))
        val info = repo.getCacheInfo()
        assertEquals(8L, info.sizeBytes)
        assertEquals(2, info.entryCount)
    }

    @Test
    fun getCacheInfo_reports_zero_for_empty_cache() = runTest {
        val repo = CacheRepositoryImpl(LocalCacheDataSource(FakeDao()))
        val info = repo.getCacheInfo()
        assertEquals(0L, info.sizeBytes)
        assertEquals(0, info.entryCount)
    }

    @Test
    fun clearAll_empties_cache() = runTest {
        val dao = FakeDao()
        dao.rows.add(row("a", "x", 1L))
        val repo = CacheRepositoryImpl(LocalCacheDataSource(dao))
        repo.clearAll()
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun evictToSize_removes_unpinned_until_under_budget() = runTest {
        val dao = FakeDao()
        // Each json is 10 bytes; total 30 bytes, all unpinned.
        dao.rows.add(row("old", "0123456789", 1L))
        dao.rows.add(row("mid", "0123456789", 2L))
        dao.rows.add(row("new", "0123456789", 3L))
        val repo = CacheRepositoryImpl(LocalCacheDataSource(dao))
        repo.evictToSize(15L)
        // Eviction batch removes oldest-first unpinned entries until under budget.
        assertTrue(dao.rows.sumOf { it.responseJson.length } <= 15)
    }

    @Test
    fun evictToSize_keeps_entries_already_under_budget() = runTest {
        val dao = FakeDao()
        dao.rows.add(row("a", "0123456789", 1L))
        val repo = CacheRepositoryImpl(LocalCacheDataSource(dao))
        repo.evictToSize(100L)
        assertEquals(listOf("a"), dao.rows.map { it.cacheKey })
    }

    @Test
    fun evictToSize_retains_pinned_entries() = runTest {
        val dao = FakeDao()
        dao.rows.add(row("pinned", "0123456789", 1L, pinned = true))
        dao.rows.add(row("loose", "0123456789", 2L))
        val repo = CacheRepositoryImpl(LocalCacheDataSource(dao))
        // Budget below total but pinned cannot be evicted; loop must terminate.
        repo.evictToSize(5L)
        assertTrue(dao.rows.any { it.cacheKey == "pinned" })
    }
}
