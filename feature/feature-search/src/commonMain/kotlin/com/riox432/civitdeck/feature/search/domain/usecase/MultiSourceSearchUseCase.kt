package com.riox432.civitdeck.feature.search.domain.usecase

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelSearchQuery
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.PaginatedResult
import com.riox432.civitdeck.domain.repository.HuggingFaceRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.TensorArtRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

/**
 * Aggregates search results from multiple model sources (CivitAI, HuggingFace, TensorArt)
 * with parallel fetching and graceful per-source error handling.
 *
 * Each source pages independently through [MultiSourceSearchContinuation]; a source is fetched
 * only while it is selected, not exhausted, and has not failed in this session.
 */
class MultiSourceSearchUseCase(
    private val modelRepository: ModelRepository,
    private val huggingFaceRepository: HuggingFaceRepository,
    private val tensorArtRepository: TensorArtRepository,
) {
    /**
     * Fetches the next page of every fetchable source in [selectedSources].
     *
     * @param continuation where each source continues from; null starts every source at its first page.
     * @param civitaiQuery the Civitai filters to send; its cursor and limit are replaced per page.
     * @throws MultiSourceSearchException when every source fetched by this call failed.
     */
    suspend operator fun invoke(
        query: String? = null,
        selectedSources: Set<ModelSource> = setOf(ModelSource.CIVITAI),
        continuation: MultiSourceSearchContinuation? = null,
        limit: Int = 20,
        civitaiQuery: ModelSearchQuery = ModelSearchQuery(query = query),
    ): MultiSourceSearchResult = supervisorScope {
        val current = continuation ?: MultiSourceSearchContinuation()
        fun shouldFetch(source: ModelSource) = source in selectedSources && current.canFetch(source)

        val civitaiDeferred = if (shouldFetch(ModelSource.CIVITAI)) {
            async { fetchCivitai(civitaiQuery, current.civitaiCursor, limit) }
        } else {
            null
        }

        val huggingFaceDeferred = if (shouldFetch(ModelSource.HUGGING_FACE)) {
            async { fetchHuggingFace(query, current.huggingFaceOffset, limit) }
        } else {
            null
        }

        val tensorArtDeferred = if (shouldFetch(ModelSource.TENSOR_ART)) {
            async { fetchTensorArt(query, current.tensorArtPage, limit) }
        } else {
            null
        }

        val civitaiResult = civitaiDeferred?.awaitCatching()
        val huggingFaceResult = huggingFaceDeferred?.awaitCatching()
        val tensorArtResult = tensorArtDeferred?.awaitCatching()

        val errors = buildMap {
            civitaiResult?.exceptionOrNull()?.let { put(ModelSource.CIVITAI, it) }
            huggingFaceResult?.exceptionOrNull()?.let { put(ModelSource.HUGGING_FACE, it) }
            tensorArtResult?.exceptionOrNull()?.let { put(ModelSource.TENSOR_ART, it) }
        }
        val fetchedCount = listOfNotNull(civitaiResult, huggingFaceResult, tensorArtResult).size
        if (fetchedCount > 0 && errors.size == fetchedCount) {
            throw MultiSourceSearchException(errors)
        }

        val civitaiPage = civitaiResult?.getOrNull()
        val huggingFaceModels = huggingFaceResult?.getOrNull()
        val tensorArtModels = tensorArtResult?.getOrNull()

        val next = current
            .copy(failedSources = current.failedSources + errors.keys)
            .advance(civitaiPage, huggingFaceModels, tensorArtModels, limit)

        MultiSourceSearchResult(
            models = mergeResults(
                civitaiPage?.items.orEmpty(),
                huggingFaceModels.orEmpty(),
                tensorArtModels.orEmpty(),
            ),
            continuation = next,
            hasMore = next.canContinue(selectedSources),
            sourceErrors = errors,
        )
    }

    private suspend fun fetchCivitai(
        query: ModelSearchQuery,
        cursor: String?,
        limit: Int,
    ): PaginatedResult<Model> = modelRepository.getModels(query.copy(cursor = cursor, limit = limit))

    private suspend fun fetchHuggingFace(
        query: String?,
        offset: Int,
        limit: Int,
    ): List<Model> = huggingFaceRepository.searchModels(
        query = query,
        limit = limit,
        offset = offset,
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

/** Moves each source that returned a page (non-null) past it; the others keep their position. */
private fun MultiSourceSearchContinuation.advance(
    civitaiPage: PaginatedResult<Model>?,
    huggingFaceModels: List<Model>?,
    tensorArtModels: List<Model>?,
    limit: Int,
): MultiSourceSearchContinuation {
    var next = this
    if (civitaiPage != null) {
        val nextCursor = civitaiPage.metadata.nextCursor
        next = next.copy(
            civitaiCursor = nextCursor,
            civitaiExhausted = nextCursor == null || nextCursor == civitaiCursor,
        )
    }
    // A short raw page is the provider's last; a full one may be followed by an empty page.
    if (huggingFaceModels != null) {
        next = next.copy(
            huggingFaceOffset = huggingFaceOffset + limit,
            huggingFaceExhausted = huggingFaceModels.size < limit,
        )
    }
    if (tensorArtModels != null) {
        next = next.copy(
            tensorArtPage = tensorArtPage + 1,
            tensorArtExhausted = tensorArtModels.size < limit,
        )
    }
    return next
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
 * Awaits a [Deferred] and wraps the result in [Result], catching any exception so that other
 * sources are not affected. Cancellation is rethrown so a cancelled search never reports it as a
 * source failure.
 */
private suspend fun <T> Deferred<T>.awaitCatching(): Result<T> =
    try {
        Result.success(await())
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        Result.failure(e)
    }

/** Result of a multi-source search operation. */
data class MultiSourceSearchResult(
    val models: List<Model>,
    val continuation: MultiSourceSearchContinuation,
    /** Whether any selected source can still return another page in this session. */
    val hasMore: Boolean,
    val sourceErrors: Map<ModelSource, Throwable>,
) {
    val hasPartialFailure: Boolean get() = sourceErrors.isNotEmpty()
}

/** Thrown when every source a multi-source search fetched failed; [sourceErrors] holds each failure. */
class MultiSourceSearchException(
    val sourceErrors: Map<ModelSource, Throwable>,
) : Exception(
    sourceErrors.values.mapNotNull { it.message }.distinct().joinToString("; ").ifEmpty { null },
    sourceErrors.values.firstOrNull(),
)
