package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.ModelUpdateNotificationDao
import com.riox432.civitdeck.data.local.entity.ModelUpdateNotificationEntity
import com.riox432.civitdeck.domain.model.ModelUpdate
import com.riox432.civitdeck.domain.model.UpdateSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [ModelUpdateNotificationRepositoryImpl] covering save (with source mapping
 * and empty-list short-circuit), unread count, mark-read, and entity-to-domain mapping.
 */
class ModelUpdateNotificationRepositoryImplTest {

    private class FakeDao : ModelUpdateNotificationDao {
        val entities = mutableListOf<ModelUpdateNotificationEntity>()
        private var idCounter = 1L
        private val allFlow = MutableStateFlow<List<ModelUpdateNotificationEntity>>(emptyList())
        private val unreadFlow = MutableStateFlow(0)

        private fun emit() {
            allFlow.value = entities.sortedByDescending { it.createdAt }.toList()
            unreadFlow.value = entities.count { !it.isRead }
        }

        override suspend fun insertAll(entities: List<ModelUpdateNotificationEntity>) {
            entities.forEach { this.entities.add(it.copy(id = idCounter++)) }
            emit()
        }

        override fun observeAll(): Flow<List<ModelUpdateNotificationEntity>> = allFlow

        override fun observeUnreadCount(): Flow<Int> = unreadFlow

        override suspend fun markRead(id: Long): Int {
            val idx = entities.indexOfFirst { it.id == id }
            if (idx < 0) return 0
            entities[idx] = entities[idx].copy(isRead = true)
            emit()
            return 1
        }

        override suspend fun markAllRead(): Int {
            val count = entities.count { !it.isRead }
            for (i in entities.indices) entities[i] = entities[i].copy(isRead = true)
            emit()
            return count
        }

        override suspend fun deleteOlderThan(threshold: Long): Int {
            val before = entities.size
            entities.removeAll { it.createdAt < threshold }
            emit()
            return before - entities.size
        }
    }

    private fun update(modelId: Long) = ModelUpdate(
        modelId = modelId,
        modelName = "Model$modelId",
        newVersionName = "v2",
        newVersionId = 200L + modelId,
    )

    @Test
    fun saveNotifications_inserts_with_source() = runTest {
        val dao = FakeDao()
        val repo = ModelUpdateNotificationRepositoryImpl(dao)
        repo.saveNotifications(listOf(update(1L)), UpdateSource.FOLLOWED)
        assertEquals(1, dao.entities.size)
        assertEquals("FOLLOWED", dao.entities[0].source)
    }

    @Test
    fun saveNotifications_ignores_empty_list() = runTest {
        val dao = FakeDao()
        val repo = ModelUpdateNotificationRepositoryImpl(dao)
        repo.saveNotifications(emptyList(), UpdateSource.FAVORITE)
        assertTrue(dao.entities.isEmpty())
    }

    @Test
    fun observeNotifications_maps_source_string_to_enum() = runTest {
        val dao = FakeDao()
        val repo = ModelUpdateNotificationRepositoryImpl(dao)
        repo.saveNotifications(listOf(update(1L)), UpdateSource.FOLLOWED)
        val result = repo.observeNotifications().first()
        assertEquals(UpdateSource.FOLLOWED, result[0].source)
        assertEquals("Model1", result[0].modelName)
    }

    @Test
    fun observeUnreadCount_reflects_unread_entries() = runTest {
        val dao = FakeDao()
        val repo = ModelUpdateNotificationRepositoryImpl(dao)
        repo.saveNotifications(listOf(update(1L), update(2L)), UpdateSource.FAVORITE)
        assertEquals(2, repo.observeUnreadCount().first())
    }

    @Test
    fun markRead_decrements_unread_count() = runTest {
        val dao = FakeDao()
        val repo = ModelUpdateNotificationRepositoryImpl(dao)
        repo.saveNotifications(listOf(update(1L), update(2L)), UpdateSource.FAVORITE)
        repo.markRead(dao.entities[0].id)
        assertEquals(1, repo.observeUnreadCount().first())
    }

    @Test
    fun markAllRead_clears_unread_count() = runTest {
        val dao = FakeDao()
        val repo = ModelUpdateNotificationRepositoryImpl(dao)
        repo.saveNotifications(listOf(update(1L), update(2L)), UpdateSource.FAVORITE)
        repo.markAllRead()
        assertEquals(0, repo.observeUnreadCount().first())
    }
}
