package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import com.riox432.civitdeck.domain.model.ComfyUIHistoryPage
import kotlinx.coroutines.flow.Flow

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
}
