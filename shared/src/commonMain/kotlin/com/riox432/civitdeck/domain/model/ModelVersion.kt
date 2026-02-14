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
