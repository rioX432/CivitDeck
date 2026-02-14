package com.riox432.civitdeck.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageListResponse(
    val items: List<ImageDto>,
    val metadata: PaginationMetadataDto,
)

@Serializable
data class ImageDto(
    val id: Long = 0,
    val url: String = "",
    val hash: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val nsfw: Boolean = false,
    val nsfwLevel: String? = null,
    val createdAt: String? = null,
    val postId: Long? = null,
    val username: String? = null,
    val stats: ImageStatsDto? = null,
    val meta: ImageMetaDto? = null,
)

@Serializable
data class ImageStatsDto(
    val cryCount: Int = 0,
    val laughCount: Int = 0,
    val likeCount: Int = 0,
    val heartCount: Int = 0,
    val commentCount: Int = 0,
)

@Serializable
data class ImageMetaDto(
    val prompt: String? = null,
    val negativePrompt: String? = null,
    val sampler: String? = null,
    val cfgScale: Double? = null,
    val steps: Int? = null,
    val seed: Long? = null,
    @SerialName("Model")
    val model: String? = null,
    @SerialName("Size")
    val size: String? = null,
)
