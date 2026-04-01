package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.util.Logger

/**
 * Finds visually similar models by searching CivitAI with the source model's
 * tags, type, and baseModel as search criteria. Results are ranked by download
 * count (most popular first) and the source model is excluded from results.
 */
class GetSimilarModelsUseCase(private val repository: ModelRepository) {

    suspend operator fun invoke(sourceModel: Model, limit: Int = RESULT_LIMIT): List<Model> {
        val tags = sourceModel.tags.take(MAX_TAG_QUERIES)
        val baseModel = sourceModel.modelVersions.firstOrNull()?.baseModel
        val candidateMap = mutableMapOf<Long, Model>()

        fetchByTags(tags, sourceModel, candidateMap)
        fetchByBaseModel(baseModel, sourceModel, candidateMap)

        return rankCandidates(candidateMap.values, sourceModel.tags, baseModel, limit)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchByTags(tags: List<String>, source: Model, out: MutableMap<Long, Model>) {
        for (tag in tags) {
            try {
                val result = repository.getModels(
                    tag = tag,
                    type = source.type,
                    sort = SortOrder.MostDownloaded,
                    limit = PER_TAG_LIMIT,
                )
                addCandidates(result.items, source.id, out)
            } catch (e: Exception) {
                Logger.w(TAG, "Similar models tag query failed for '$tag': ${e.message}")
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchByBaseModel(baseModel: String?, source: Model, out: MutableMap<Long, Model>) {
        if (baseModel == null) return
        val baseModelEnum = BaseModel.entries.firstOrNull { it.apiValue == baseModel } ?: return
        try {
            val result = repository.getModels(
                type = source.type,
                baseModels = listOf(baseModelEnum),
                sort = SortOrder.MostDownloaded,
                limit = PER_TAG_LIMIT,
            )
            addCandidates(result.items, source.id, out)
        } catch (e: Exception) {
            Logger.w(TAG, "Similar models base model query failed: ${e.message}")
        }
    }

    private fun addCandidates(models: List<Model>, excludeId: Long, out: MutableMap<Long, Model>) {
        for (model in models) {
            if (model.id != excludeId) out[model.id] = model
        }
    }

    private fun rankCandidates(
        candidates: Collection<Model>,
        sourceTags: List<String>,
        baseModel: String?,
        limit: Int,
    ): List<Model> {
        val sourceTagSet = sourceTags.toSet()
        return candidates
            .map { candidate ->
                val tagOverlap = candidate.tags.count { it in sourceTagSet }
                val sameBase = if (baseModel != null &&
                    candidate.modelVersions.firstOrNull()?.baseModel == baseModel
                ) {
                    1
                } else {
                    0
                }
                candidate to tagOverlap * TAG_WEIGHT + sameBase * BASE_MODEL_WEIGHT
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    companion object {
        private const val TAG = "GetSimilarModelsUseCase"
        private const val MAX_TAG_QUERIES = 3
        private const val PER_TAG_LIMIT = 20
        private const val RESULT_LIMIT = 20
        private const val TAG_WEIGHT = 2
        private const val BASE_MODEL_WEIGHT = 1
    }
}
