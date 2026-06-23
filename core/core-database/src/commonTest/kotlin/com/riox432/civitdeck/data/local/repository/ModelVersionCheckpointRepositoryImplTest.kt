package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.ModelVersionCheckpointDao
import com.riox432.civitdeck.data.local.entity.ModelVersionCheckpointEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [ModelVersionCheckpointRepositoryImpl] covering single/batch save,
 * checkpoint lookups, the (versionId, checkedAt) map shape, and stale cleanup.
 */
class ModelVersionCheckpointRepositoryImplTest {

    private class FakeDao : ModelVersionCheckpointDao {
        val rows = mutableListOf<ModelVersionCheckpointEntity>()

        override suspend fun getCheckpoint(modelId: Long): ModelVersionCheckpointEntity? =
            rows.firstOrNull { it.modelId == modelId }

        override suspend fun getAllCheckpoints(): List<ModelVersionCheckpointEntity> = rows.toList()

        override suspend fun upsert(entity: ModelVersionCheckpointEntity) {
            rows.removeAll { it.modelId == entity.modelId }
            rows.add(entity)
        }

        override suspend fun upsertAll(entities: List<ModelVersionCheckpointEntity>) {
            entities.forEach { upsert(it) }
        }

        override suspend fun delete(modelId: Long): Int {
            val before = rows.size
            rows.removeAll { it.modelId == modelId }
            return before - rows.size
        }

        override suspend fun deleteStaleCheckpoints(activeModelIds: List<Long>): Int {
            val before = rows.size
            rows.removeAll { it.modelId !in activeModelIds }
            return before - rows.size
        }
    }

    @Test
    fun saveCheckpoint_stores_version_id() = runTest {
        val repo = ModelVersionCheckpointRepositoryImpl(FakeDao())
        repo.saveCheckpoint(1L, 42L)
        assertEquals(42L, repo.getCheckpoint(1L))
    }

    @Test
    fun saveCheckpoint_overwrites_existing() = runTest {
        val repo = ModelVersionCheckpointRepositoryImpl(FakeDao())
        repo.saveCheckpoint(1L, 42L)
        repo.saveCheckpoint(1L, 99L)
        assertEquals(99L, repo.getCheckpoint(1L))
    }

    @Test
    fun getCheckpoint_returns_null_when_absent() = runTest {
        val repo = ModelVersionCheckpointRepositoryImpl(FakeDao())
        assertNull(repo.getCheckpoint(7L))
    }

    @Test
    fun saveCheckpoints_stores_batch() = runTest {
        val repo = ModelVersionCheckpointRepositoryImpl(FakeDao())
        repo.saveCheckpoints(mapOf(1L to 10L, 2L to 20L))
        assertEquals(10L, repo.getCheckpoint(1L))
        assertEquals(20L, repo.getCheckpoint(2L))
    }

    @Test
    fun getAllCheckpoints_maps_to_version_and_timestamp_pair() = runTest {
        val repo = ModelVersionCheckpointRepositoryImpl(FakeDao())
        repo.saveCheckpoint(1L, 10L)
        val all = repo.getAllCheckpoints()
        assertEquals(10L, all[1L]?.first)
        assertTrue((all[1L]?.second ?: 0L) > 0L)
    }

    @Test
    fun deleteStaleCheckpoints_keeps_only_active_ids() = runTest {
        val repo = ModelVersionCheckpointRepositoryImpl(FakeDao())
        repo.saveCheckpoints(mapOf(1L to 10L, 2L to 20L, 3L to 30L))
        repo.deleteStaleCheckpoints(setOf(1L, 3L))
        assertNull(repo.getCheckpoint(2L))
        assertEquals(10L, repo.getCheckpoint(1L))
        assertEquals(30L, repo.getCheckpoint(3L))
    }
}
