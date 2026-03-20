package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.comfyhub.ComfyHubApi
import com.riox432.civitdeck.data.api.comfyhub.ComfyHubWorkflowDto
import com.riox432.civitdeck.data.api.comfyui.ComfyUIApi
import com.riox432.civitdeck.domain.model.ComfyHubCategory
import com.riox432.civitdeck.domain.model.ComfyHubSortOrder
import com.riox432.civitdeck.domain.model.ComfyHubWorkflow
import com.riox432.civitdeck.domain.repository.ComfyHubRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class ComfyHubRepositoryImpl(
    private val comfyHubApi: ComfyHubApi,
    private val comfyUIApi: ComfyUIApi,
    private val json: Json,
) : ComfyHubRepository {

    override suspend fun searchWorkflows(
        query: String,
        category: ComfyHubCategory,
        sort: ComfyHubSortOrder,
        page: Int,
    ): List<ComfyHubWorkflow> {
        val response = comfyHubApi.searchWorkflows(
            query = query,
            category = category.displayName,
            sort = sort.displayName,
            page = page,
        )
        return response.items.map { it.toDomain() }
    }

    override suspend fun getWorkflowDetail(workflowId: String): ComfyHubWorkflow {
        return comfyHubApi.getWorkflowDetail(workflowId).toDomain()
    }

    override suspend fun importToServer(workflowJson: String): String {
        val workflow = json.decodeFromString<JsonObject>(workflowJson)
        val response = comfyUIApi.submitPrompt(workflow)
        return response.promptId
    }

    private fun ComfyHubWorkflowDto.toDomain(): ComfyHubWorkflow = ComfyHubWorkflow(
        id = id,
        name = name,
        description = description,
        author = creator?.username ?: "Unknown",
        tags = tags,
        category = category,
        previewImageUrl = previewImageUrl,
        nodeCount = nodeCount,
        downloads = downloads,
        rating = rating,
        workflowJson = workflowJson,
    )
}
