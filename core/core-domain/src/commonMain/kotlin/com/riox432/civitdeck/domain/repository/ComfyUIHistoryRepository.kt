package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ComfyUIBoundHistoryItem
import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.domain.model.ComfyUIHistoryPage
import com.riox432.civitdeck.domain.model.ComfyUIJobHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository for accessing ComfyUI generation history.
 * Fetches completed prompt outputs from the ComfyUI /history endpoint.
 */
interface ComfyUIHistoryRepository {
    /**
     * Returns the generated images of the newest [maxItems] history entries.
     * Emits a fresh page on each call.
     */
    fun fetchHistory(maxItems: Int): Flow<ComfyUIHistoryPage>

    /**
     * Returns generated images for a single prompt by [promptId].
     * Emits an empty list if the prompt is not found or not yet completed.
     */
    fun fetchHistoryItem(promptId: String): Flow<List<ComfyUIGeneratedImage>>

    /**
     * Returns the history of [job] from [job]'s own server, with its remote outcome and presence.
     * Fails when that server's saved connection was deleted or changed; never reads the active one.
     *
     * The default fails on collection so an implementation without target support cannot
     * answer from the active server through the prompt-ID overload.
     */
    fun fetchHistoryItem(job: ComfyUIJobHandle): Flow<ComfyUIBoundHistoryItem> = flow {
        throw UnsupportedOperationException("This repository does not support bound history lookups")
    }
}
