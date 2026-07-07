package com.riox432.civitdeck.domain.model

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
    val source: ModelSource = ModelSource.CIVITAI,
)

data class ModelStats(
    val downloadCount: Int,
    val favoriteCount: Int,
    val commentCount: Int,
    val ratingCount: Int,
    val rating: Double,
)

/**
 * Preview candidates for browse-surface cards, safest first.
 *
 * Videos are excluded — image loaders cannot decode a video URL, which
 * previously rendered NSFW models (whose first preview is often a video)
 * as broken cards. Static images are ordered by ascending NSFW level so a
 * SFW preview is preferred when the creator provides one; original order
 * is kept within the same level.
 */
fun Model.browseThumbnailCandidates(): List<ModelImage> =
    modelVersions.firstOrNull()?.images.orEmpty()
        .filter { it.contentType == MediaContentType.IMAGE }
        .sortedBy { it.nsfwLevel.ordinal }
