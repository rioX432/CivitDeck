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
    /** True when at least one version is behind a live CivitAI paid-access gate (download needs a purchase). */
    val hasActivePaidAccess: Boolean = false,
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
 * Raw video URLs are never returned — image loaders cannot decode them, which
 * previously rendered NSFW models (whose first preview is often a video)
 * as broken cards. When the latest version has no static image at all, its
 * CivitAI-hosted videos are offered as still frames instead, so video-only
 * models do not render blank. Candidates are ordered by ascending NSFW level
 * so a SFW preview is preferred when the creator provides one; original order
 * is kept within the same level.
 */
fun Model.browseThumbnailCandidates(): List<ModelImage> {
    val previews = modelVersions.firstOrNull()?.images.orEmpty()
    val images = previews.filter { it.contentType == MediaContentType.IMAGE }
    val candidates = images.ifEmpty {
        previews
            .filter { it.contentType == MediaContentType.VIDEO }
            .mapNotNull { video ->
                val stillUrl = video.url.civitaiVideoStillUrl() ?: return@mapNotNull null
                video.copy(url = stillUrl, contentType = MediaContentType.IMAGE)
            }
    }
    return candidates.sortedBy { it.nsfwLevel.ordinal }
}
