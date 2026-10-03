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
    val pickleScanResult: String? = null,
    val virusScanResult: String? = null,
    val scannedAt: String? = null,
)

data class ModelImage(
    val url: String,
    val nsfw: Boolean,
    val nsfwLevel: NsfwLevel,
    val width: Int,
    val height: Int,
    val hash: String?,
    val meta: ImageGenerationMeta?,
    val contentType: MediaContentType = MediaContentType.IMAGE,
)

/**
 * Returns a CDN URL resized to the given [width].
 * CivitAI CDN format: .../xG1nkqKTMzGDvpLrqFT7WA/{uuid}/width={size}/{filename}
 */
fun ModelImage.thumbnailUrl(width: Int = 450): String = url.cdnThumbnailUrl(width)

/**
 * String variant of [thumbnailUrl] for call sites that only carry a raw URL
 * (e.g. fullscreen viewers referencing the grid's cached thumbnail).
 */
fun String.cdnThumbnailUrl(width: Int = 450): String {
    if (!contains(CIVITAI_IMAGE_HOST)) return this
    val parts = split("/").toMutableList()
    // The last segment is the file name, never a transform.
    val transformIndices = 0 until parts.lastIndex
    val widthIdx = transformIndices.firstOrNull { i -> parts[i].split(",").any { it.startsWith("width=") } }
    // A comma-separated transform (e.g. a video still frame) must stay one segment:
    // a second width segment in front of it makes the CDN return the original video.
    val transformListIdx = transformIndices.firstOrNull { i -> parts[i].isCdnTransformList() }
    when {
        widthIdx != null -> parts[widthIdx] = parts[widthIdx].withCdnWidth(width)
        transformListIdx != null -> parts[transformListIdx] = "${parts[transformListIdx]},width=$width"
        parts.size > CDN_TRANSFORM_INDEX -> parts.add(CDN_TRANSFORM_INDEX, "width=$width")
    }
    return parts.joinToString("/")
}

private fun String.withCdnWidth(width: Int): String =
    split(",").joinToString(",") { if (it.startsWith("width=")) "width=$width" else it }

private fun String.isCdnTransformList(): Boolean = contains(',') && split(",").all { '=' in it }

/**
 * Still-frame JPEG URL for a video hosted on the CivitAI image CDN, or null for any other host.
 * The CDN transcodes a video to a still only when the transform carries `anim=false,transcode=true`
 * and the file name ends in `.jpeg`; the video's own transforms are dropped.
 */
internal fun String.civitaiVideoStillUrl(): String? {
    val url = substringBefore('?')
    if (!url.startsWith("https://$CIVITAI_IMAGE_HOST/")) return null
    val parts = url.split("/")
    if (parts.size <= CDN_TRANSFORM_INDEX) return null
    val token = parts[CDN_TRANSFORM_INDEX - 2]
    val uuid = parts[CDN_TRANSFORM_INDEX - 1]
    val baseName = parts.last().substringBeforeLast('.')
    if (token.isEmpty() || uuid.isEmpty() || baseName.isEmpty()) return null
    return "https://$CIVITAI_IMAGE_HOST/$token/$uuid/$STILL_FRAME_TRANSFORM/$baseName.jpeg"
}

private const val CIVITAI_IMAGE_HOST = "image.civitai.com"

/** Index of the transform segment in a split CDN URL: https: / "" / host / token / uuid / transform. */
private const val CDN_TRANSFORM_INDEX = 5

private const val STILL_FRAME_TRANSFORM = "anim=false,transcode=true"

/** Strip CDN width parameter so raw and thumbnail URLs can be compared. */
fun String.stripCdnWidth(): String {
    if (!contains("image.civitai.com")) return this
    return split("/").filter { !it.startsWith("width=") }.joinToString("/")
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
