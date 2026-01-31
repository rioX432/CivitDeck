package com.omooooori.civitdeck.domain.model

data class ModelVersion(
    val id: Long,
    val modelId: Long,
    val name: String,
    val description: String?,
    val createdAt: String,
    val baseModel: String?,
    val trainedWords: List<String>,
    val downloadUrl: String,
    val files: List<ModelFile>,
    val images: List<ModelImage>,
    val stats: ModelVersionStats?,
)

data class ModelVersionStats(
    val downloadCount: Int,
    val ratingCount: Int,
    val rating: Double,
)

data class ModelFile(
    val id: Long,
    val name: String,
    val sizeKB: Double,
    val type: String?,
    val format: String?,
    val fp: String?,
    val size: String?,
    val downloadUrl: String,
    val primary: Boolean,
    val hashes: Map<String, String>,
)

data class ModelImage(
    val url: String,
    val nsfw: Boolean,
    val width: Int,
    val height: Int,
    val hash: String?,
    val meta: ImageGenerationMeta?,
)
