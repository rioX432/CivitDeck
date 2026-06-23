package com.riox432.civitdeck.feature.externalserver.data.repository

import com.riox432.civitdeck.data.local.dao.ExternalServerConfigDao
import com.riox432.civitdeck.data.local.entity.ExternalServerConfigEntity
import com.riox432.civitdeck.domain.model.ExternalServerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [ExternalServerConfigRepositoryImpl]'s entity <-> domain mapping plus the
 * insert-vs-update branch in [ExternalServerConfigRepositoryImpl.saveConfig] and the
 * deactivate-then-activate ordering in [ExternalServerConfigRepositoryImpl.activateConfig].
 */
class ExternalServerConfigRepositoryImplTest {

    private class FakeDao : ExternalServerConfigDao {
        val entities = mutableListOf<ExternalServerConfigEntity>()
        private var idCounter = 1L
        private val updates = MutableStateFlow(0)

        override fun observeAll(): Flow<List<ExternalServerConfigEntity>> =
            updates.map { entities.sortedByDescending { it.createdAt } }

        override fun observeActive(): Flow<ExternalServerConfigEntity?> =
            updates.map { entities.firstOrNull { it.isActive } }

        override suspend fun getActive(): ExternalServerConfigEntity? =
            entities.firstOrNull { it.isActive }

        override suspend fun getById(id: Long): ExternalServerConfigEntity? =
            entities.firstOrNull { it.id == id }

        override suspend fun getAll(): List<ExternalServerConfigEntity> =
            entities.sortedByDescending { it.createdAt }

        override suspend fun insert(entity: ExternalServerConfigEntity): Long {
            val newId = idCounter++
            entities.add(entity.copy(id = newId))
            updates.value++
            return newId
        }

        override suspend fun insertAll(entities: List<ExternalServerConfigEntity>) {
            this.entities.addAll(entities)
            updates.value++
        }

        override suspend fun update(entity: ExternalServerConfigEntity): Int {
            val idx = entities.indexOfFirst { it.id == entity.id }
            if (idx >= 0) entities[idx] = entity
            updates.value++
            return if (idx >= 0) 1 else 0
        }

        override suspend fun deactivateAll(): Int {
            val count = entities.count { it.isActive }
            entities.replaceAll { it.copy(isActive = false) }
            updates.value++
            return count
        }

        override suspend fun activate(id: Long): Int {
            val idx = entities.indexOfFirst { it.id == id }
            if (idx >= 0) entities[idx] = entities[idx].copy(isActive = true)
            updates.value++
            return if (idx >= 0) 1 else 0
        }

        override suspend fun updateTestResult(id: Long, testedAt: Long, success: Boolean): Int {
            val idx = entities.indexOfFirst { it.id == id }
            if (idx >= 0) {
                entities[idx] = entities[idx].copy(lastTestedAt = testedAt, lastTestSuccess = success)
            }
            updates.value++
            return if (idx >= 0) 1 else 0
        }

        override suspend fun deleteById(id: Long): Int {
            val count = entities.count { it.id == id }
            entities.removeAll { it.id == id }
            updates.value++
            return count
        }

        override suspend fun deleteAll(): Int {
            val count = entities.size
            entities.clear()
            updates.value++
            return count
        }
    }

    private fun domainConfig(
        id: Long = 0L,
        name: String = "Home",
        baseUrl: String = "http://localhost:8080",
        isActive: Boolean = false,
    ) = ExternalServerConfig(
        id = id,
        name = name,
        baseUrl = baseUrl,
        apiKey = "secret",
        isActive = isActive,
        lastTestedAt = null,
        lastTestSuccess = null,
        createdAt = 0L,
    )

    @Test
    fun observeConfigs_maps_entities_to_domain() = runTest {
        val dao = FakeDao()
        dao.entities.add(
            ExternalServerConfigEntity(
                id = 1L, name = "Server A", baseUrl = "http://a", apiKey = "k1",
                isActive = true, createdAt = 1000L,
            ),
        )
        val repo = ExternalServerConfigRepositoryImpl(dao)

        val result = repo.observeConfigs().first()

        assertEquals(1, result.size)
        assertEquals("Server A", result[0].name)
        assertEquals("http://a", result[0].baseUrl)
        assertEquals("k1", result[0].apiKey)
        assertTrue(result[0].isActive)
    }

    @Test
    fun observeActiveConfig_returns_null_when_none_active() = runTest {
        val dao = FakeDao()
        dao.entities.add(
            ExternalServerConfigEntity(
                id = 1L, name = "Inactive", baseUrl = "http://a", isActive = false, createdAt = 1L,
            ),
        )
        val repo = ExternalServerConfigRepositoryImpl(dao)

        assertNull(repo.observeActiveConfig().first())
    }

    @Test
    fun saveConfig_with_zero_id_inserts_and_returns_new_id() = runTest {
        val dao = FakeDao()
        val repo = ExternalServerConfigRepositoryImpl(dao)

        val newId = repo.saveConfig(domainConfig(id = 0L))

        assertEquals(1L, newId)
        assertEquals(1, dao.entities.size)
        // createdAt of 0L is replaced with a real timestamp on insert.
        assertTrue(dao.entities[0].createdAt > 0L)
    }

    @Test
    fun saveConfig_with_existing_id_updates_and_returns_same_id() = runTest {
        val dao = FakeDao()
        dao.entities.add(
            ExternalServerConfigEntity(
                id = 5L, name = "Old", baseUrl = "http://old", isActive = false, createdAt = 100L,
            ),
        )
        val repo = ExternalServerConfigRepositoryImpl(dao)

        val returnedId = repo.saveConfig(domainConfig(id = 5L, name = "New", baseUrl = "http://new"))

        assertEquals(5L, returnedId)
        assertEquals(1, dao.entities.size)
        assertEquals("New", dao.entities[0].name)
        assertEquals("http://new", dao.entities[0].baseUrl)
    }

    @Test
    fun deleteConfig_removes_entity() = runTest {
        val dao = FakeDao()
        dao.entities.add(
            ExternalServerConfigEntity(
                id = 1L, name = "A", baseUrl = "http://a", isActive = false, createdAt = 1L,
            ),
        )
        val repo = ExternalServerConfigRepositoryImpl(dao)

        repo.deleteConfig(1L)

        assertTrue(dao.entities.isEmpty())
    }

    @Test
    fun activateConfig_deactivates_others_then_activates_target() = runTest {
        val dao = FakeDao()
        dao.entities.add(
            ExternalServerConfigEntity(
                id = 1L, name = "A", baseUrl = "http://a", isActive = true, createdAt = 1L,
            ),
        )
        dao.entities.add(
            ExternalServerConfigEntity(
                id = 2L, name = "B", baseUrl = "http://b", isActive = false, createdAt = 2L,
            ),
        )
        val repo = ExternalServerConfigRepositoryImpl(dao)

        repo.activateConfig(2L)

        assertFalse(dao.entities.first { it.id == 1L }.isActive)
        assertTrue(dao.entities.first { it.id == 2L }.isActive)
    }

    @Test
    fun updateTestResult_records_timestamp_and_success_flag() = runTest {
        val dao = FakeDao()
        dao.entities.add(
            ExternalServerConfigEntity(
                id = 1L, name = "A", baseUrl = "http://a", isActive = false, createdAt = 1L,
            ),
        )
        val repo = ExternalServerConfigRepositoryImpl(dao)

        repo.updateTestResult(1L, success = true)

        val updated = dao.entities.first { it.id == 1L }
        assertNotNull(updated.lastTestedAt)
        assertTrue(updated.lastTestedAt!! > 0L)
        assertEquals(true, updated.lastTestSuccess)
    }
}
