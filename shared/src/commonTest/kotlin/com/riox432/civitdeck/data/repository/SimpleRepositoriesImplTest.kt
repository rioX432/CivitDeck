package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.dao.ExcludedTagDao
import com.riox432.civitdeck.data.local.dao.HiddenModelDao
import com.riox432.civitdeck.data.local.dao.SavedPromptDao
import com.riox432.civitdeck.data.local.entity.ExcludedTagEntity
import com.riox432.civitdeck.data.local.entity.HiddenModelEntity
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleRepositoriesImplTest {

    // --- ExcludedTagRepositoryImpl ---

    private class FakeExcludedTagDao : ExcludedTagDao {
        val entities = mutableListOf<ExcludedTagEntity>()

        override suspend fun getAll(): List<ExcludedTagEntity> = entities.toList()

        override suspend fun insert(entity: ExcludedTagEntity) {
            if (entities.none { it.tag == entity.tag }) entities.add(entity)
        }

        override suspend fun delete(tag: String) {
            entities.removeAll { it.tag == tag }
        }
    }

    @Test
    fun excludedTag_getAll_maps_to_strings() = runTest {
        val dao = FakeExcludedTagDao()
        dao.entities.add(ExcludedTagEntity(tag = "nsfw", addedAt = 1000L))
        dao.entities.add(ExcludedTagEntity(tag = "gore", addedAt = 2000L))
        val repo = ExcludedTagRepositoryImpl(dao)
        assertEquals(listOf("nsfw", "gore"), repo.getExcludedTags())
    }

    @Test
    fun excludedTag_add_inserts() = runTest {
        val dao = FakeExcludedTagDao()
        val repo = ExcludedTagRepositoryImpl(dao)
        repo.addExcludedTag("violence")
        assertEquals(1, dao.entities.size)
        assertEquals("violence", dao.entities[0].tag)
    }

    @Test
    fun excludedTag_remove_deletes() = runTest {
        val dao = FakeExcludedTagDao()
        dao.entities.add(ExcludedTagEntity(tag = "nsfw", addedAt = 1000L))
        val repo = ExcludedTagRepositoryImpl(dao)
        repo.removeExcludedTag("nsfw")
        assertTrue(dao.entities.isEmpty())
    }

    // --- HiddenModelRepositoryImpl ---

    private class FakeHiddenModelDao : HiddenModelDao {
        val entities = mutableListOf<HiddenModelEntity>()

        override suspend fun getAllIds(): List<Long> = entities.map { it.modelId }
        override suspend fun getAll(): List<HiddenModelEntity> =
            entities.sortedByDescending { it.hiddenAt }

        override suspend fun insert(entity: HiddenModelEntity) {
            if (entities.none { it.modelId == entity.modelId }) entities.add(entity)
        }

        override suspend fun delete(modelId: Long) {
            entities.removeAll { it.modelId == modelId }
        }
    }

    @Test
    fun hiddenModel_getIds_returns_set() = runTest {
        val dao = FakeHiddenModelDao()
        dao.entities.add(HiddenModelEntity(modelId = 1L, modelName = "A", hiddenAt = 1000L))
        dao.entities.add(HiddenModelEntity(modelId = 2L, modelName = "B", hiddenAt = 2000L))
        val repo = HiddenModelRepositoryImpl(dao)
        assertEquals(setOf(1L, 2L), repo.getHiddenModelIds())
    }

    @Test
    fun hiddenModel_hide_inserts() = runTest {
        val dao = FakeHiddenModelDao()
        val repo = HiddenModelRepositoryImpl(dao)
        repo.hideModel(42L, "Hidden Model")
        assertEquals(1, dao.entities.size)
        assertEquals(42L, dao.entities[0].modelId)
        assertEquals("Hidden Model", dao.entities[0].modelName)
    }

    @Test
    fun hiddenModel_unhide_deletes() = runTest {
        val dao = FakeHiddenModelDao()
        dao.entities.add(HiddenModelEntity(modelId = 1L, modelName = "A", hiddenAt = 1000L))
        val repo = HiddenModelRepositoryImpl(dao)
        repo.unhideModel(1L)
        assertTrue(dao.entities.isEmpty())
    }

    // --- SavedPromptRepositoryImpl ---

    private class FakeSavedPromptDao : SavedPromptDao {
        val entities = mutableListOf<SavedPromptEntity>()
        private var idCounter = 1L
        private val updates = MutableStateFlow(0)

        override fun observeAll(): Flow<List<SavedPromptEntity>> =
            updates.map { entities.sortedByDescending { it.savedAt } }

        override suspend fun insert(entity: SavedPromptEntity) {
            entities.add(entity.copy(id = idCounter++))
            updates.value++
        }

        override suspend fun deleteById(id: Long) {
            entities.removeAll { it.id == id }
            updates.value++
        }
    }

    @Test
    fun savedPrompt_observeAll_maps_to_domain() = runTest {
        val dao = FakeSavedPromptDao()
        dao.entities.add(
            SavedPromptEntity(
                id = 1L, prompt = "1girl", negativePrompt = null, sampler = null,
                steps = null, cfgScale = null, seed = null, modelName = null,
                size = null, sourceImageUrl = null, savedAt = 1000L,
            ),
        )
        val repo = SavedPromptRepositoryImpl(dao)
        val result = repo.observeAll().first()
        assertEquals(1, result.size)
        assertEquals("1girl", result[0].prompt)
    }

    @Test
    fun savedPrompt_save_maps_meta_to_entity() = runTest {
        val dao = FakeSavedPromptDao()
        val repo = SavedPromptRepositoryImpl(dao)
        val meta = ImageGenerationMeta(
            prompt = "test prompt",
            negativePrompt = "bad",
            sampler = "Euler",
            cfgScale = 7.0,
            steps = 20,
            seed = 123L,
            model = "ModelName",
            size = "512x512",
        )
        repo.save(meta, "https://example.com/img.jpg")
        assertEquals(1, dao.entities.size)
        assertEquals("test prompt", dao.entities[0].prompt)
        assertEquals("bad", dao.entities[0].negativePrompt)
        assertEquals("ModelName", dao.entities[0].modelName)
        assertEquals("https://example.com/img.jpg", dao.entities[0].sourceImageUrl)
    }

    @Test
    fun savedPrompt_delete_removes() = runTest {
        val dao = FakeSavedPromptDao()
        dao.entities.add(
            SavedPromptEntity(
                id = 1L, prompt = "test", negativePrompt = null, sampler = null,
                steps = null, cfgScale = null, seed = null, modelName = null,
                size = null, sourceImageUrl = null, savedAt = 1000L,
            ),
        )
        val repo = SavedPromptRepositoryImpl(dao)
        repo.delete(1L)
        assertTrue(dao.entities.isEmpty())
    }
}
