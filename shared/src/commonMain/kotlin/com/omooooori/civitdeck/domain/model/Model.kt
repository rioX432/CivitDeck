package com.omooooori.civitdeck.domain.model

data class Model(
    val id: Long,
    val name: String,
    val description: String?,
    val type: ModelType,
    val nsfw: Boolean,
    val tags: List<String>,
    val mode: ModelMode?,
    val creator: Creator?,
    val stats: ModelStats,
    val modelVersions: List<ModelVersion>,
)

data class ModelStats(
    val downloadCount: Int,
    val favoriteCount: Int,
    val commentCount: Int,
    val ratingCount: Int,
    val rating: Double,
)
