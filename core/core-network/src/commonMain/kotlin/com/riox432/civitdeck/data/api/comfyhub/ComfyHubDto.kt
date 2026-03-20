package com.riox432.civitdeck.data.api.comfyhub

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ComfyHubWorkflowDto(
    val id: String,
    val name: String,
    val description: String = "",
    val creator: ComfyHubCreatorDto? = null,
    val tags: List<String> = emptyList(),
    val category: String = "Other",
    @SerialName("preview_image_url") val previewImageUrl: String? = null,
    @SerialName("node_count") val nodeCount: Int = 0,
    val downloads: Int = 0,
    val rating: Double = 0.0,
    @SerialName("workflow_json") val workflowJson: String = "{}",
)

@Serializable
data class ComfyHubCreatorDto(
    val username: String = "",
)

@Serializable
data class ComfyHubSearchResponse(
    val items: List<ComfyHubWorkflowDto> = emptyList(),
    val metadata: ComfyHubPagination? = null,
)

@Serializable
data class ComfyHubPagination(
    val totalItems: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
)
