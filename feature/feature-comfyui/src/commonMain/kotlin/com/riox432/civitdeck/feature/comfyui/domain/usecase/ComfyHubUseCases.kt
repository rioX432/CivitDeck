package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.ComfyHubCategory
import com.riox432.civitdeck.domain.model.ComfyHubSortOrder
import com.riox432.civitdeck.domain.model.ComfyHubWorkflow
import com.riox432.civitdeck.domain.repository.ComfyHubRepository

class SearchComfyHubWorkflowsUseCase(
    private val repository: ComfyHubRepository,
) {
    suspend operator fun invoke(
        query: String = "",
        category: ComfyHubCategory = ComfyHubCategory.ALL,
        sort: ComfyHubSortOrder = ComfyHubSortOrder.MOST_DOWNLOADED,
        page: Int = 1,
    ): List<ComfyHubWorkflow> = repository.searchWorkflows(query, category, sort, page)
}

class GetComfyHubWorkflowDetailUseCase(
    private val repository: ComfyHubRepository,
) {
    suspend operator fun invoke(workflowId: String): ComfyHubWorkflow =
        repository.getWorkflowDetail(workflowId)
}

class ImportComfyHubWorkflowUseCase(
    private val repository: ComfyHubRepository,
) {
    suspend operator fun invoke(workflowJson: String): String =
        repository.importToServer(workflowJson)
}
