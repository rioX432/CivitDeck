package com.omooooori.civitdeck.data.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModelListResponse(
    val items: List<ModelResponse>,
    val metadata: PaginationMetadataDto,
)

@Serializable
data class ModelResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
    val type: String,
    val nsfw: Boolean = false,
    val tags: List<String> = emptyList(),
    val mode: String? = null,
    val creator: ModelCreatorDto? = null,
    val stats: ModelStatsDto? = null,
    val modelVersions: List<ModelVersionDto> = emptyList(),
)

@Serializable
data class ModelCreatorDto(
    val username: String,
    val image: String? = null,
)

@Serializable
data class ModelStatsDto(
    val downloadCount: Int = 0,
    val favoriteCount: Int = 0,
    val commentCount: Int = 0,
    val ratingCount: Int = 0,
    val rating: Double = 0.0,
)
