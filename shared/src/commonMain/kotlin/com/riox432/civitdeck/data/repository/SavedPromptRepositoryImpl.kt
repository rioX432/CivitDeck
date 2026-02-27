package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.SavedPromptDao
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedPromptRepositoryImpl(
    private val dao: SavedPromptDao,
) : SavedPromptRepository {

    override fun observeAll(): Flow<List<SavedPrompt>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTemplates(): Flow<List<SavedPrompt>> =
        dao.observeTemplates().map { list -> list.map { it.toDomain() } }

    override fun observeHistory(): Flow<List<SavedPrompt>> =
        dao.observeHistory().map { list -> list.map { it.toDomain() } }

    override fun search(query: String): Flow<List<SavedPrompt>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun save(meta: ImageGenerationMeta, sourceImageUrl: String?) {
        dao.insert(meta.toEntity(sourceImageUrl, autoSaved = false))
    }

    override suspend fun autoSave(meta: ImageGenerationMeta, sourceImageUrl: String?) {
        val prompt = meta.prompt ?: return
        val count = dao.countByPromptAndModel(prompt, meta.model)
        if (count > 0) return
        dao.insert(meta.toEntity(sourceImageUrl, autoSaved = true))
    }

    override suspend fun toggleTemplate(id: Long, isTemplate: Boolean, templateName: String?) {
        dao.updateTemplate(id, isTemplate, templateName)
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)

    private fun ImageGenerationMeta.toEntity(
        sourceImageUrl: String?,
        autoSaved: Boolean,
    ) = SavedPromptEntity(
        prompt = prompt ?: "",
        negativePrompt = negativePrompt,
        sampler = sampler,
        steps = steps,
        cfgScale = cfgScale,
        seed = seed,
        modelName = model,
        size = size,
        sourceImageUrl = sourceImageUrl,
        savedAt = currentTimeMillis(),
        autoSaved = autoSaved,
    )

    private fun SavedPromptEntity.toDomain() = SavedPrompt(
        id = id,
        prompt = prompt,
        negativePrompt = negativePrompt,
        sampler = sampler,
        steps = steps,
        cfgScale = cfgScale,
        seed = seed,
        modelName = modelName,
        size = size,
        sourceImageUrl = sourceImageUrl,
        savedAt = savedAt,
        isTemplate = isTemplate,
        templateName = templateName,
        autoSaved = autoSaved,
    )
}
