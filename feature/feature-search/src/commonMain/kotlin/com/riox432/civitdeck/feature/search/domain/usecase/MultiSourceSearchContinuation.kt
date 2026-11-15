package com.riox432.civitdeck.feature.search.domain.usecase

import com.riox432.civitdeck.domain.model.ModelSource

/**
 * Where each provider of a multi-source search continues from. Every provider pages on its own,
 * so one provider running out (or failing) never stops the others.
 *
 * The default value starts every provider at its first page.
 *
 * @property failedSources providers whose last request failed. They are not fetched again in this
 *   search session; starting a new session (Refresh or a filter change) retries them.
 */
data class MultiSourceSearchContinuation(
    val civitaiCursor: String? = null,
    val civitaiExhausted: Boolean = false,
    val huggingFaceOffset: Int = 0,
    val huggingFaceExhausted: Boolean = false,
    val tensorArtPage: Int = FIRST_TENSOR_ART_PAGE,
    val tensorArtExhausted: Boolean = false,
    val failedSources: Set<ModelSource> = emptySet(),
) {
    /** Whether [source] may still be fetched in this session. */
    fun canFetch(source: ModelSource): Boolean = source !in failedSources && !isExhausted(source)

    /** Whether any of [selectedSources] may still be fetched in this session. */
    fun canContinue(selectedSources: Set<ModelSource>): Boolean = selectedSources.any(::canFetch)

    private fun isExhausted(source: ModelSource): Boolean = when (source) {
        ModelSource.CIVITAI -> civitaiExhausted
        ModelSource.HUGGING_FACE -> huggingFaceExhausted
        ModelSource.TENSOR_ART -> tensorArtExhausted
    }

    companion object {
        const val FIRST_TENSOR_ART_PAGE = 1
    }
}
