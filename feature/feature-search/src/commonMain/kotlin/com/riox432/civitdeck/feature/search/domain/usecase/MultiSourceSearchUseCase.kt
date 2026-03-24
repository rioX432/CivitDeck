package com.riox432.civitdeck.feature.search.domain.usecase

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.repository.HuggingFaceRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.TensorArtRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

/**
 * Aggregates search results from multiple model sources (CivitAI, HuggingFace, TensorArt)
 * with parallel fetching and graceful per-source error handling.
 */
class MultiSourceSearchUseCase(
    private val modelRepository: ModelRepository,
    private val huggingFaceRepository: HuggingFaceRepository,
    private val tensorArtRepository: TensorArtRepository,
) {
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        query: String? = null,
        selectedSources: Set<ModelSource> = setOf(ModelSource.CIVITAI),
        cursor: String? = null,
        page: Int = 1,
        limit: Int = 20,
    ): MultiSourceSearchResult = supervisorScope {
        val civitaiDeferred = if (ModelSource.CIVITAI in selectedSources) {
            async { fetchCivitai(query, cursor, limit) }
        } else {
            null
        }

        val huggingFaceDeferred = if (ModelSource.HUGGING_FACE in selectedSources) {
            async { fetchHuggingFace(query, page, limit) }
        } else {
            null
        }

        val tensorArtDeferred = if (ModelSource.TENSOR_ART in selectedSources) {
            async { fetchTensorArt(query, page, limit) }
        } else {
            null
        }

        val civitaiResult = civitaiDeferred?.awaitCatching()
        val huggingFaceResult = huggingFaceDeferred?.awaitCatching()
        val tensorArtResult = tensorArtDeferred?.awaitCatching()

        val civitaiModels = civitaiResult?.getOrNull()?.items.orEmpty()
        val huggingFaceModels = huggingFaceResult?.getOrNull().orEmpty()
        val tensorArtModels = tensorArtResult?.getOrNull().orEmpty()

        val errors = buildMap {
            civitaiResult?.exceptionOrNull()?.let { put(ModelSource.CIVITAI, it) }
            huggingFaceResult?.exceptionOrNull()?.let { put(ModelSource.HUGGING_FACE, it) }
            tensorArtResult?.exceptionOrNull()?.let { put(ModelSource.TENSOR_ART, it) }
        }

        val merged = mergeResults(civitaiModels, huggingFaceModels, tensorArtModels)
        val nextCursor = civitaiResult?.getOrNull()?.metadata?.nextCursor

        MultiSourceSearchResult(
            models = merged,
            nextCursor = nextCursor,
            sourceErrors = errors,
        )
    }

    private suspend fun fetchCivitai(
        query: String?,
        cursor: String?,
        limit: Int,
    ): PaginatedResult<Model> = modelRepository.getModels(
        query = query,
        cursor = cursor,
        limit = limit,
    )

    private suspend fun fetchHuggingFace(
        query: String?,
        page: Int,
        limit: Int,
    ): List<Model> = huggingFaceRepository.searchModels(
        query = query,
        limit = limit,
        offset = (page - 1) * limit,
    )

    private suspend fun fetchTensorArt(
        query: String?,
        page: Int,
        limit: Int,
    ): List<Model> = tensorArtRepository.searchModels(
        query = query ?: "",
        page = page,
        pageSize = limit,
    )
}

/**
 * Merges results with CivitAI as primary source, then interleaves secondary sources.
 * CivitAI results come first, followed by alternating HuggingFace and TensorArt items.
 */
private fun mergeResults(
    civitai: List<Model>,
    huggingFace: List<Model>,
    tensorArt: List<Model>,
): List<Model> {
    val secondary = interleave(huggingFace, tensorArt)
    return civitai + secondary
}

/** Interleaves two lists, taking one element from each in turn. */
private fun interleave(a: List<Model>, b: List<Model>): List<Model> {
    val result = mutableListOf<Model>()
    val maxSize = maxOf(a.size, b.size)
    for (i in 0 until maxSize) {
        if (i < a.size) result.add(a[i])
        if (i < b.size) result.add(b[i])
    }
    return result
}

/**
 * Awaits a [kotlinx.coroutines.Deferred] and wraps the result in [Result],
 * catching any exception so that other sources are not affected.
 */
private suspend fun <T> kotlinx.coroutines.Deferred<T>.awaitCatching(): Result<T> =
    try {
        Result.success(await())
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Result.failure(e)
    }

/** Result of a multi-source search operation. */
data class MultiSourceSearchResult(
    val models: List<Model>,
    val nextCursor: String?,
    val sourceErrors: Map<ModelSource, Throwable>,
) {
    val hasPartialFailure: Boolean get() = sourceErrors.isNotEmpty()
}
