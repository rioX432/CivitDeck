package com.riox432.civitdeck.data.api.dto

import com.riox432.civitdeck.domain.model.Creator
import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.ImageStats
import com.riox432.civitdeck.domain.model.MediaContentType
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelFile
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelMode
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.ModelVersionStats
import com.riox432.civitdeck.domain.model.NsfwLevel
import com.riox432.civitdeck.domain.model.PageMetadata
import com.riox432.civitdeck.domain.model.Tag

fun ModelResponse.toDomain(): Model = Model(
    id = id,
    name = name,
    description = description,
    type = type.toModelType(),
    nsfw = nsfw,
    tags = tags,
    mode = mode?.toModelMode(),
    creator = creator?.toDomain(),
    stats = stats?.toDomain() ?: ModelStats(0, 0, 0, 0, 0.0),
    modelVersions = modelVersions.map { it.toDomain() },
    source = ModelSource.CIVITAI,
)

fun ModelVersionDto.toDomain(): ModelVersion = ModelVersion(
    id = id,
    modelId = modelId,
    name = name,
    description = description,
    createdAt = createdAt ?: "",
    baseModel = baseModel,
    trainedWords = trainedWords,
    downloadUrl = downloadUrl,
    files = files.map { it.toDomain() },
    images = images.map { it.toDomain() },
    stats = stats?.toDomain(),
)

fun ModelStatsDto.toDomain(): ModelStats {
    // CivitAI API v1 replaced favoriteCount/rating with thumbsUpCount/thumbsDownCount.
    // Map thumbs to the domain model's favoriteCount/rating for backward compatibility.
    val effectiveFavoriteCount = if (thumbsUpCount > 0) thumbsUpCount else favoriteCount
    val totalThumbs = thumbsUpCount + thumbsDownCount
    val effectiveRatingCount = if (totalThumbs > 0) totalThumbs else ratingCount
    val effectiveRating = when {
        totalThumbs > 0 -> thumbsUpCount.toDouble() / totalThumbs * 4.0 + 1.0 // scale to 1-5
        rating > 0 -> rating
        else -> 0.0
    }
    return ModelStats(
        downloadCount = downloadCount,
        favoriteCount = effectiveFavoriteCount,
        commentCount = commentCount,
        ratingCount = effectiveRatingCount,
        rating = effectiveRating,
    )
}

fun ModelVersionStatsDto.toDomain(): ModelVersionStats {
    val totalThumbs = thumbsUpCount + thumbsDownCount
    val effectiveRatingCount = if (totalThumbs > 0) totalThumbs else ratingCount
    val effectiveRating = when {
        totalThumbs > 0 -> thumbsUpCount.toDouble() / totalThumbs * 4.0 + 1.0
        rating > 0 -> rating
        else -> 0.0
    }
    return ModelVersionStats(
        downloadCount = downloadCount,
        ratingCount = effectiveRatingCount,
        rating = effectiveRating,
    )
}

fun ModelFileDto.toDomain(): ModelFile = ModelFile(
    id = id,
    name = name,
    sizeKB = sizeKB,
    type = type,
    format = metadata?.format,
    fp = metadata?.fp,
    size = metadata?.size,
    downloadUrl = downloadUrl,
    primary = primary,
    hashes = hashes,
    pickleScanResult = pickleScanResult,
    virusScanResult = virusScanResult,
    scannedAt = scannedAt,
)

fun ModelImageDto.toDomain(): ModelImage = ModelImage(
    url = url,
    nsfw = nsfw,
    nsfwLevel = nsfwLevel.toNsfwLevel(),
    width = width,
    height = height,
    hash = hash,
    meta = meta?.toDomain(),
    contentType = MediaContentType.fromApiType(type) ?: MediaContentType.fromUrl(url),
)

fun ModelCreatorDto.toDomain(): Creator = Creator(
    username = username,
    image = image,
    modelCount = null,
    link = null,
)

fun ImageDto.toDomain(): Image = Image(
    id = id,
    url = url,
    hash = hash,
    width = width,
    height = height,
    nsfw = nsfw,
    nsfwLevel = nsfwLevel.toNsfwLevel(),
    createdAt = createdAt ?: "",
    postId = postId,
    username = username,
    stats = stats?.toDomain(),
    meta = meta?.toDomain(),
    contentType = MediaContentType.fromApiType(type) ?: MediaContentType.fromUrl(url),
)

fun ImageStatsDto.toDomain(): ImageStats = ImageStats(
    cryCount = cryCount,
    laughCount = laughCount,
    likeCount = likeCount,
    heartCount = heartCount,
    commentCount = commentCount,
)

fun ImageMetaDto.toDomain(): ImageGenerationMeta = ImageGenerationMeta(
    prompt = prompt,
    negativePrompt = negativePrompt,
    sampler = sampler,
    cfgScale = cfgScale,
    steps = steps,
    seed = seed,
    model = model,
    size = size,
    additionalParams = additionalParams,
)

fun CreatorDto.toDomain(): Creator = Creator(
    username = username,
    image = null,
    modelCount = modelCount,
    link = link,
)

fun TagDto.toDomain(): Tag = Tag(
    name = name,
    modelCount = modelCount,
    link = link,
)

fun PaginationMetadataDto.toDomain(): PageMetadata = PageMetadata(
    nextCursor = nextCursor,
    nextPage = nextPage,
)

private inline fun <reified T : Enum<T>> String.toEnum(default: T): T = try {
    enumValueOf(this)
} catch (_: IllegalArgumentException) {
    default
}

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? = this?.let {
    try {
        enumValueOf<T>(it)
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun String.toModelType(): ModelType = toEnum(ModelType.Other)

private fun String?.toModelMode(): ModelMode? = toEnumOrNull<ModelMode>()

private fun String?.toNsfwLevel(): NsfwLevel = this?.toEnum(NsfwLevel.None) ?: NsfwLevel.None

private fun Int?.toNsfwLevel(): NsfwLevel = when (this) {
    1 -> NsfwLevel.None
    2 -> NsfwLevel.Soft
    4 -> NsfwLevel.Mature
    8 -> NsfwLevel.X
    else -> NsfwLevel.None
}
