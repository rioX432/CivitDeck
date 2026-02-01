package com.riox432.civitdeck.data.api.dto

import com.riox432.civitdeck.domain.model.Creator
import com.riox432.civitdeck.domain.model.Image
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.ImageStats
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelFile
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelMode
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

fun ModelStatsDto.toDomain(): ModelStats = ModelStats(
    downloadCount = downloadCount,
    favoriteCount = favoriteCount,
    commentCount = commentCount,
    ratingCount = ratingCount,
    rating = rating,
)

fun ModelVersionStatsDto.toDomain(): ModelVersionStats = ModelVersionStats(
    downloadCount = downloadCount,
    ratingCount = ratingCount,
    rating = rating,
)

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
)

fun ModelImageDto.toDomain(): ModelImage = ModelImage(
    url = url,
    nsfw = nsfw,
    width = width,
    height = height,
    hash = hash,
    meta = meta?.toDomain(),
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

private fun String.toModelType(): ModelType = try {
    ModelType.valueOf(this)
} catch (_: IllegalArgumentException) {
    ModelType.Other
}

private fun String?.toModelMode(): ModelMode? = this?.let {
    try {
        ModelMode.valueOf(it)
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun String?.toNsfwLevel(): NsfwLevel = this?.let {
    try {
        NsfwLevel.valueOf(it)
    } catch (_: IllegalArgumentException) {
        NsfwLevel.None
    }
} ?: NsfwLevel.None
