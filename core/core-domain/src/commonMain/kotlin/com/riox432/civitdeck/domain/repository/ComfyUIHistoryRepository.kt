package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ComfyUIGeneratedImage
import kotlinx.coroutines.flow.Flow

/**
 * Repository for accessing ComfyUI generation history.
 * Fetches completed prompt outputs from the ComfyUI /history endpoint.
 */
interface ComfyUIHistoryRepository {
    /**
     * Returns all generated images across all completed prompts.
     * Emits a fresh list on each call.
     */
    fun fetchHistory(): Flow<List<ComfyUIGeneratedImage>>

    /**
     * Returns generated images for a single prompt by [promptId].
     * Emits an empty list if the prompt is not found or not yet completed.
     */
    fun fetchHistoryItem(promptId: String): Flow<List<ComfyUIGeneratedImage>>
}
