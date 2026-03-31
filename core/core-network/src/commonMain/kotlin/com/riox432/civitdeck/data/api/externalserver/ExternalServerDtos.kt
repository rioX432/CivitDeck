package com.riox432.civitdeck.data.api.externalserver

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CapabilitiesResponseDto(
    val endpoints: List<String> = emptyList(),
    val version: String = "",
    val name: String = "",
)

@Serializable
data class ServerImageDto(
    val id: Int,
    @SerialName("cloud_key") val cloudKey: String = "",
    val file: String,
    @SerialName("thumb_url") val thumbUrl: String? = null,
    val character: String? = null,
    val costume: String? = null,
    val scenario: String? = null,
    val nsfw: Boolean = false,
    val selected: Boolean = false,
    @SerialName("post_status") val postStatus: String? = null,
    @SerialName("aesthetic_score") val aestheticScore: Float? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val seed: Long? = null,
    val prompt: String? = null,
)

@Serializable
data class PaginatedImagesResponseDto(
    val images: List<ServerImageDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 96,
    @SerialName("total_pages") val totalPages: Int = 0,
)

// Generation option types
@Serializable
data class GenerationOptionsResponseDto(
    val options: List<GenerationOptionDto> = emptyList(),
)

@Serializable
data class GenerationOptionDto(
    val key: String,
    val label: String,
    val type: String, // "select", "text", "number"
    val choices: List<GenerationChoiceDto> = emptyList(),
    @SerialName("depends_on") val dependsOn: String? = null,
    @SerialName("choices_endpoint") val choicesEndpoint: String? = null,
    val placeholder: String? = null,
    val default: kotlinx.serialization.json.JsonPrimitive? = null,
    val min: Int? = null,
    val max: Int? = null,
)

@Serializable
data class GenerationChoiceDto(
    val value: String,
    val label: String,
    val description: String? = null,
)

@Serializable
data class GenerationExecuteRequestDto(
    val params: Map<String, String>,
)

@Serializable
data class GenerationExecuteResponseDto(
    @SerialName("job_id") val jobId: String,
    val status: String,
    val message: String = "",
)

@Serializable
data class DeleteImageRequestDto(
    @SerialName("cloud_key") val cloudKey: String,
)

@Serializable
data class BulkDeleteRequestDto(
    @SerialName("cloud_keys") val cloudKeys: List<String>,
)

@Serializable
data class DeleteResponseDto(
    val ok: Boolean = false,
)

@Serializable
data class GenerationStatusResponseDto(
    @SerialName("job_id") val jobId: String,
    val status: String, // "queued", "running", "completed", "error"
    val progress: Float = 0f,
    val completed: Int = 0,
    val total: Int = 0,
    val message: String = "",
)
