package com.riox432.civitdeck.feature.prompts.data.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.SavedPromptDao
import com.riox432.civitdeck.data.local.dao.SavedReferenceDao
import com.riox432.civitdeck.data.local.entity.SavedPromptEntity
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.model.SourceReference
import com.riox432.civitdeck.domain.model.SourceReferenceCodec
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import com.riox432.civitdeck.domain.repository.SavedReferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [referenceDao] is optional only so existing single-argument callers keep compiling; without it
 * the reference operations throw and [saveTemplate] falls back to a plain upsert that does not
 * preserve a stored snapshot. Production wiring always supplies it.
 */
class SavedPromptRepositoryImpl(
    private val dao: SavedPromptDao,
    private val referenceDao: SavedReferenceDao? = null,
) : SavedPromptRepository, SavedReferenceRepository {

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

    override suspend fun delete(id: Long) { dao.deleteById(id) }

    override suspend fun saveTemplate(prompt: SavedPrompt) {
        val entity = prompt.toEntity()
        if (referenceDao != null) referenceDao.upsertPreservingSource(entity) else dao.upsert(entity)
    }

    override suspend fun saveReference(reference: SourceReference): Long =
        requireReferenceDao().insert(reference.toEntity())

    override suspend fun getReference(id: Long): SavedPrompt? =
        requireReferenceDao().getById(id)?.toDomain()

    override fun observeReference(id: Long): Flow<SavedPrompt?> =
        requireReferenceDao().observeById(id).map { it?.toDomain() }

    private fun requireReferenceDao(): SavedReferenceDao =
        checkNotNull(referenceDao) { "SavedReferenceDao is required for saved reference operations" }

    // The snapshot is left out: only saveReference writes it, and the DAO keeps the stored one.
    private fun SavedPrompt.toEntity() = SavedPromptEntity(
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
        templateVariables = templateVariables,
        templateType = templateType,
        templateMetadata = templateMetadata,
        isAppMode = isAppMode,
        rawWorkflowJson = rawWorkflowJson,
    )

    private fun SourceReference.toEntity() = SavedPromptEntity(
        prompt = generation?.prompt.orEmpty(),
        negativePrompt = generation?.negativePrompt,
        sampler = generation?.sampler,
        steps = generation?.steps,
        cfgScale = generation?.cfgScale,
        seed = generation?.seed,
        modelName = generation?.model,
        size = generation?.size,
        sourceImageUrl = mediaUrl,
        savedAt = currentTimeMillis(),
        sourceReferenceJson = SourceReferenceCodec.encode(this),
    )

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
        templateVariables = templateVariables,
        templateType = templateType,
        templateMetadata = templateMetadata,
        isAppMode = isAppMode,
        rawWorkflowJson = rawWorkflowJson,
        sourceReference = sourceReferenceJson?.let(SourceReferenceCodec::decode),
    )
}
