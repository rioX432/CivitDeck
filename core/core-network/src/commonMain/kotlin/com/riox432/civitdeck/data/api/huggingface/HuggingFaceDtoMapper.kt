package com.riox432.civitdeck.data.api.huggingface

import com.riox432.civitdeck.domain.model.Creator
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelReference
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.ModelStats
import com.riox432.civitdeck.domain.model.ModelType
import io.ktor.http.encodeURLPathPart
import kotlin.math.abs

fun HuggingFaceModelDto.toDomain(): Model = Model(
    id = generateNumericId(modelId),
    name = modelId.substringAfter("/", modelId),
    description = null,
    type = mapModelType(pipelineTag, tags),
    nsfw = false,
    tags = tags,
    mode = null,
    creator = author?.let {
        Creator(
            username = it,
            image = null,
            modelCount = null,
            link = "$HF_BASE_URL/$it",
        )
    },
    stats = ModelStats(
        downloadCount = downloads,
        favoriteCount = likes,
        commentCount = 0,
        ratingCount = 0,
        rating = 0.0,
    ),
    modelVersions = emptyList(),
    source = ModelSource.HUGGING_FACE,
    reference = huggingFaceReference(modelId),
)

/**
 * Keeps the full `owner/repository` ID. A repository ID without an owner (e.g. `gpt2`) is
 * still a valid Hugging Face path; dot segments or empty parts are not, so they get no URL.
 */
private fun huggingFaceReference(modelId: String): ModelReference {
    if (modelId.isBlank()) {
        return ModelReference.legacy(ModelSource.HUGGING_FACE, generateNumericId(modelId))
    }
    val segments = modelId.split("/")
    val urlSafe = segments.size <= MAX_REPO_ID_SEGMENTS &&
        segments.none { it.isEmpty() || it == "." || it == ".." }
    val url = if (urlSafe) {
        "$HF_BASE_URL/" + segments.joinToString("/") { it.encodeURLPathPart() }
    } else {
        null
    }
    return ModelReference(source = ModelSource.HUGGING_FACE, nativeId = modelId, canonicalUrl = url)
}

/**
 * Generate a stable numeric ID from the HuggingFace model ID string.
 * Uses the absolute value of hashCode to produce a positive Long.
 */
private fun generateNumericId(modelId: String): Long =
    abs(modelId.hashCode().toLong())

/**
 * Map HuggingFace pipeline_tag and tags to the closest [ModelType].
 */
private fun mapModelType(
    pipelineTag: String?,
    tags: List<String>,
): ModelType = when {
    pipelineTag == "text-to-image" -> ModelType.Checkpoint
    tags.any { it.equals("lora", ignoreCase = true) } -> ModelType.LORA
    tags.any { it.equals("locon", ignoreCase = true) } -> ModelType.LoCon
    tags.any { it.equals("textual-inversion", ignoreCase = true) } -> ModelType.TextualInversion
    tags.any { it.equals("controlnet", ignoreCase = true) } -> ModelType.Controlnet
    tags.any { it.equals("vae", ignoreCase = true) } -> ModelType.VAE
    tags.any { it.equals("upscaler", ignoreCase = true) } -> ModelType.Upscaler
    else -> ModelType.Checkpoint
}

private const val HF_BASE_URL = "https://huggingface.co"
private const val MAX_REPO_ID_SEGMENTS = 2
