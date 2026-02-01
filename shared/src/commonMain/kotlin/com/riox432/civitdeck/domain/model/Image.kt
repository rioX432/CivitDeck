package com.riox432.civitdeck.domain.model

data class Image(
    val id: Long,
    val url: String,
    val hash: String?,
    val width: Int,
    val height: Int,
    val nsfw: Boolean,
    val nsfwLevel: NsfwLevel,
    val createdAt: String,
    val postId: Long?,
    val username: String?,
    val stats: ImageStats?,
    val meta: ImageGenerationMeta?,
)

data class ImageStats(
    val cryCount: Int,
    val laughCount: Int,
    val likeCount: Int,
    val heartCount: Int,
    val commentCount: Int,
)

data class ImageGenerationMeta(
    val prompt: String?,
    val negativePrompt: String?,
    val sampler: String?,
    val cfgScale: Double?,
    val steps: Int?,
    val seed: Long?,
    val model: String?,
    val size: String?,
)
