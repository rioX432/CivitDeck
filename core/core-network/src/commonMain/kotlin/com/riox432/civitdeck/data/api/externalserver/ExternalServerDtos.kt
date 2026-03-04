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
