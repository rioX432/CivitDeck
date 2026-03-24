package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.repository.ModelRepository

class EnrichModelImagesUseCase(private val repository: ModelRepository) {
    suspend operator fun invoke(
        modelVersionId: Long,
        images: List<ModelImage>,
    ): List<ModelImage> {
        val version = repository.getModelVersion(modelVersionId)
        val metaByImageId = version.images
            .filter { it.meta != null }
            .mapNotNull { img -> extractImageId(img.url)?.let { it to img.meta } }
            .toMap()
        return images.map { image ->
            if (image.meta == null) {
                val meta = extractImageId(image.url)?.let { metaByImageId[it] }
                if (meta != null) image.copy(meta = meta) else image
            } else {
                image
            }
        }
    }

    // URL format: https://image.civitai.com/xG1nkqKTMzGDvpLrqFT7WA/{uuid}/...
    private fun extractImageId(url: String): String? = url.split("/").getOrNull(4)
}
