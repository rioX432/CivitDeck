package com.riox432.civitdeck.feature.search.data.repository

import com.riox432.civitdeck.data.local.dao.ExcludedTagDao
import com.riox432.civitdeck.data.local.entity.ExcludedTagEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [ExcludedTagRepositoryImpl]'s DAO delegation and the projection
 * of [ExcludedTagEntity] rows down to plain tag strings.
 */
class ExcludedTagRepositoryImplTest {

    private class FakeDao : ExcludedTagDao {
        val entities = mutableListOf<ExcludedTagEntity>()

        override suspend fun getAll(): List<ExcludedTagEntity> =
            entities.sortedByDescending { it.addedAt }

        override suspend fun insert(entity: ExcludedTagEntity) {
            // Mirror OnConflictStrategy.IGNORE on the tag primary key.
            if (entities.none { it.tag == entity.tag }) entities.add(entity)
        }

        override suspend fun insertAll(entities: List<ExcludedTagEntity>) {
            entities.forEach { insert(it) }
        }

        override suspend fun deleteAll(): Int {
            val removed = entities.size
            entities.clear()
            return removed
        }

        override suspend fun delete(tag: String): Int {
            val removed = entities.count { it.tag == tag }
            entities.removeAll { it.tag == tag }
            return removed
        }
    }

    @Test
    fun getExcludedTags_maps_entities_to_tag_strings_ordered_by_recency() = runTest {
        val dao = FakeDao()
        dao.entities.add(ExcludedTagEntity(tag = "gore", addedAt = 100L))
        dao.entities.add(ExcludedTagEntity(tag = "nsfw", addedAt = 200L))
        val repo = ExcludedTagRepositoryImpl(dao)

        val result = repo.getExcludedTags()

        assertEquals(listOf("nsfw", "gore"), result) // ordered by addedAt DESC
    }

    @Test
    fun getExcludedTags_returns_empty_when_none() = runTest {
        val repo = ExcludedTagRepositoryImpl(FakeDao())

        val result = repo.getExcludedTags()

        assertTrue(result.isEmpty())
    }

    @Test
    fun addExcludedTag_inserts_new_tag() = runTest {
        val dao = FakeDao()
        val repo = ExcludedTagRepositoryImpl(dao)

        repo.addExcludedTag("violence")

        assertEquals(1, dao.entities.size)
        assertEquals("violence", dao.entities.first().tag)
    }

    @Test
    fun addExcludedTag_ignores_duplicate_tag() = runTest {
        val dao = FakeDao()
        dao.entities.add(ExcludedTagEntity(tag = "gore", addedAt = 100L))
        val repo = ExcludedTagRepositoryImpl(dao)

        repo.addExcludedTag("gore")

        assertEquals(1, dao.entities.size)
    }

    @Test
    fun removeExcludedTag_removes_matching_tag() = runTest {
        val dao = FakeDao()
        dao.entities.add(ExcludedTagEntity(tag = "gore", addedAt = 100L))
        dao.entities.add(ExcludedTagEntity(tag = "nsfw", addedAt = 200L))
        val repo = ExcludedTagRepositoryImpl(dao)

        repo.removeExcludedTag("gore")

        assertEquals(listOf("nsfw"), dao.entities.map { it.tag })
    }
}
