package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ComfyHubCategory
import com.riox432.civitdeck.domain.model.ComfyHubSortOrder
import com.riox432.civitdeck.domain.model.ComfyHubWorkflow

interface ComfyHubRepository {
    /**
     * Search published workflows with optional filters.
     */
    suspend fun searchWorkflows(
        query: String = "",
        category: ComfyHubCategory = ComfyHubCategory.ALL,
        sort: ComfyHubSortOrder = ComfyHubSortOrder.MOST_DOWNLOADED,
        page: Int = 1,
    ): List<ComfyHubWorkflow>

    /**
     * Get full workflow detail including the JSON definition.
     */
    suspend fun getWorkflowDetail(workflowId: String): ComfyHubWorkflow

    /**
     * Import a workflow JSON to the connected ComfyUI server.
     * Returns the prompt ID on success.
     */
    suspend fun importToServer(workflowJson: String): String
}
