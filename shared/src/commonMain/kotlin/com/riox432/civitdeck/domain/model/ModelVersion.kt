package com.riox432.civitdeck.domain.model

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
    val nsfwLevel: NsfwLevel,
    val width: Int,
    val height: Int,
    val hash: String?,
    val meta: ImageGenerationMeta?,
)

/**
 * Returns a CDN URL resized to the given [width].
 * CivitAI CDN format: .../xG1nkqKTMzGDvpLrqFT7WA/{uuid}/width={size}/{filename}
 */
fun ModelImage.thumbnailUrl(width: Int = 450): String {
    if (!url.contains("image.civitai.com")) return url
    val parts = url.split("/").toMutableList()
    val widthIdx = parts.indexOfFirst { it.startsWith("width=") }
    if (widthIdx != -1) {
        parts[widthIdx] = "width=$width"
    } else if (parts.size > 5) {
        parts.add(5, "width=$width")
    }
    return parts.joinToString("/")
}

fun ModelImage.isAllowed(filterLevel: NsfwFilterLevel): Boolean = when (filterLevel) {
    NsfwFilterLevel.Off -> nsfwLevel == NsfwLevel.None
    NsfwFilterLevel.Soft -> nsfwLevel == NsfwLevel.None || nsfwLevel == NsfwLevel.Soft
    NsfwFilterLevel.All -> true
}

fun List<ModelImage>.filterByNsfwLevel(filterLevel: NsfwFilterLevel): List<ModelImage> =
    if (filterLevel == NsfwFilterLevel.All) this else filter { it.isAllowed(filterLevel) }

fun List<Model>.filterNsfwImages(filterLevel: NsfwFilterLevel): List<Model> {
    if (filterLevel == NsfwFilterLevel.All) return this
    return mapNotNull { model ->
        val filteredVersions = model.modelVersions.map { version ->
            version.copy(images = version.images.filterByNsfwLevel(filterLevel))
        }
        if (filteredVersions.all { it.images.isEmpty() }) {
            null
        } else {
            model.copy(modelVersions = filteredVersions)
        }
    }
}
