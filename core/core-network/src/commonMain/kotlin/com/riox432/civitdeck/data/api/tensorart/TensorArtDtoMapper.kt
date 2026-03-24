package com.riox432.civitdeck.data.api.tensorart

import com.riox432.civitdeck.domain.model.Creator
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.NsfwLevel

/**
 * Maps TensorArt DTOs to domain models.
 */
fun TensorArtModelDto.toDomain(): Model = Model(
    id = id.tensorArtIdToLong(),
    name = name,
    description = description,
    type = type.toModelType(),
    nsfw = false,
    tags = tags,
    mode = null,
    creator = author?.toDomain(),
    stats = stats?.toDomain() ?: ModelStats(0, 0, 0, 0, 0.0),
    modelVersions = buildCoverImageVersion(),
    source = ModelSource.TENSOR_ART,
)

private fun TensorArtAuthorDto.toDomain(): Creator = Creator(
    username = name ?: "Unknown",
    image = avatar,
    modelCount = null,
    link = null,
)

private fun TensorArtStatsDto.toDomain(): ModelStats = ModelStats(
    downloadCount = downloadCount,
    favoriteCount = likeCount,
    commentCount = 0,
    ratingCount = 0,
    rating = 0.0,
)

/**
 * Creates a synthetic [ModelVersion] from the cover image URL
 * so the domain layer has image data to display.
 */
private fun TensorArtModelDto.buildCoverImageVersion(): List<ModelVersion> {
    val imageUrl = coverImage ?: return emptyList()
    val syntheticVersion = ModelVersion(
        id = id.tensorArtIdToLong(),
        modelId = id.tensorArtIdToLong(),
        name = "v1",
        description = null,
        createdAt = "",
        baseModel = baseModel,
        trainedWords = emptyList(),
        downloadUrl = "",
        files = emptyList(),
        images = listOf(
            ModelImage(
                url = imageUrl,
                nsfw = false,
                nsfwLevel = NsfwLevel.None,
                width = 0,
                height = 0,
                hash = null,
                meta = null,
            ),
        ),
        stats = null,
    )
    return listOf(syntheticVersion)
}

/**
 * Maps TensorArt type strings to domain [ModelType].
 * TensorArt uses uppercase (e.g. "CHECKPOINT", "LORA").
 */
private fun String?.toModelType(): ModelType = when (this?.uppercase()) {
    "CHECKPOINT" -> ModelType.Checkpoint
    "LORA" -> ModelType.LORA
    "LOCON" -> ModelType.LoCon
    "TEXTUALINVERSION", "TEXTUAL_INVERSION", "EMBEDDING" -> ModelType.TextualInversion
    "HYPERNETWORK" -> ModelType.Hypernetwork
    "CONTROLNET" -> ModelType.Controlnet
    "UPSCALER" -> ModelType.Upscaler
    "VAE" -> ModelType.VAE
    "POSES" -> ModelType.Poses
    else -> ModelType.Other
}

/**
 * Converts a TensorArt string ID to a Long.
 * Uses the string's hash code (absolute value) to produce a stable numeric ID.
 * Prefixes with 3 to avoid collisions with CivitAI IDs.
 */
private fun String.tensorArtIdToLong(): Long {
    return try {
        this.toLong()
    } catch (_: NumberFormatException) {
        // Use absolute hash code, prefix with 3_000_000_000 to avoid collision
        TENSOR_ART_ID_PREFIX + this.hashCode().toLong().and(ID_MASK)
    }
}

private const val TENSOR_ART_ID_PREFIX = 3_000_000_000L
private const val ID_MASK = 0x7FFFFFFFL
