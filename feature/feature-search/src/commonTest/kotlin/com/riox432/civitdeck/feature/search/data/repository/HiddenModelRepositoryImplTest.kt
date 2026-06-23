package com.riox432.civitdeck.feature.search.data.repository

import com.riox432.civitdeck.data.local.dao.HiddenModelDao
import com.riox432.civitdeck.data.local.entity.HiddenModelEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [HiddenModelRepositoryImpl]'s DAO delegation and the
 * [HiddenModelEntity] -> domain mapping for hidden model lookups.
 */
class HiddenModelRepositoryImplTest {

    private class FakeDao : HiddenModelDao {
        val entities = mutableListOf<HiddenModelEntity>()

        override suspend fun getAllIds(): List<Long> = entities.map { it.modelId }

        override suspend fun getAll(): List<HiddenModelEntity> =
            entities.sortedByDescending { it.hiddenAt }

        override suspend fun insert(entity: HiddenModelEntity) {
            // Mirror OnConflictStrategy.IGNORE on the modelId primary key.
            if (entities.none { it.modelId == entity.modelId }) entities.add(entity)
        }

        override suspend fun insertAll(entities: List<HiddenModelEntity>) {
            entities.forEach { insert(it) }
        }

        override suspend fun deleteAll(): Int {
            val removed = entities.size
            entities.clear()
            return removed
        }

        override suspend fun delete(modelId: Long): Int {
            val removed = entities.count { it.modelId == modelId }
            entities.removeAll { it.modelId == modelId }
            return removed
        }
    }

    @Test
    fun getHiddenModelIds_returns_ids_as_set() = runTest {
        val dao = FakeDao()
        dao.entities.add(HiddenModelEntity(modelId = 1, modelName = "a", hiddenAt = 100L))
        dao.entities.add(HiddenModelEntity(modelId = 2, modelName = "b", hiddenAt = 200L))
        val repo = HiddenModelRepositoryImpl(dao)

        val result = repo.getHiddenModelIds()

        assertEquals(setOf(1L, 2L), result)
    }

    @Test
    fun getHiddenModelIds_returns_empty_set_when_none_hidden() = runTest {
        val repo = HiddenModelRepositoryImpl(FakeDao())

        val result = repo.getHiddenModelIds()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getHiddenModels_maps_entities_to_domain() = runTest {
        val dao = FakeDao()
        dao.entities.add(HiddenModelEntity(modelId = 5, modelName = "anime", hiddenAt = 999L))
        val repo = HiddenModelRepositoryImpl(dao)

        val result = repo.getHiddenModels()

        assertEquals(1, result.size)
        val model = result.first()
        assertEquals(5L, model.modelId)
        assertEquals("anime", model.modelName)
        assertEquals(999L, model.hiddenAt)
    }

    @Test
    fun hideModel_inserts_entity_with_name() = runTest {
        val dao = FakeDao()
        val repo = HiddenModelRepositoryImpl(dao)

        repo.hideModel(modelId = 7, modelName = "lora")

        assertEquals(1, dao.entities.size)
        assertEquals(7L, dao.entities.first().modelId)
        assertEquals("lora", dao.entities.first().modelName)
    }

    @Test
    fun unhideModel_removes_entity_by_id() = runTest {
        val dao = FakeDao()
        dao.entities.add(HiddenModelEntity(modelId = 1, modelName = "a", hiddenAt = 100L))
        dao.entities.add(HiddenModelEntity(modelId = 2, modelName = "b", hiddenAt = 200L))
        val repo = HiddenModelRepositoryImpl(dao)

        repo.unhideModel(modelId = 1)

        assertEquals(listOf(2L), dao.entities.map { it.modelId })
    }
}
