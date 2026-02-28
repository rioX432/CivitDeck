package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.dao.SavedPromptDao
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.feature.prompts.data.repository.SavedPromptRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimpleRepositoriesImplTest {

    // --- SavedPromptRepositoryImpl ---

    private class FakeSavedPromptDao : SavedPromptDao {
        val entities = mutableListOf<SavedPromptEntity>()
        private var idCounter = 1L
        private val updates = MutableStateFlow(0)

        override fun observeAll(): Flow<List<SavedPromptEntity>> =
            updates.map { entities.sortedByDescending { it.savedAt } }

        override fun observeTemplates(): Flow<List<SavedPromptEntity>> =
            updates.map { entities.filter { it.isTemplate }.sortedByDescending { it.savedAt } }

        override fun observeHistory(): Flow<List<SavedPromptEntity>> =
            updates.map { entities.filter { it.autoSaved }.sortedByDescending { it.savedAt } }

        override fun search(query: String): Flow<List<SavedPromptEntity>> =
            updates.map { entities.filter { it.prompt.contains(query) } }

        override suspend fun countByPromptAndModel(prompt: String, modelName: String?): Int =
            entities.count { it.prompt == prompt && it.modelName == modelName }

        override suspend fun updateTemplate(id: Long, isTemplate: Boolean, templateName: String?) {
            val idx = entities.indexOfFirst { it.id == id }
            if (idx >= 0) entities[idx] = entities[idx].copy(isTemplate = isTemplate, templateName = templateName)
            updates.value++
        }


        override suspend fun insert(entity: SavedPromptEntity) {
            entities.add(entity.copy(id = idCounter++))
            updates.value++
        }

        override suspend fun upsert(entity: SavedPromptEntity) {
            val idx = entities.indexOfFirst { it.id == entity.id }
            if (idx >= 0) entities[idx] = entity else entities.add(entity.copy(id = idCounter++))
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
