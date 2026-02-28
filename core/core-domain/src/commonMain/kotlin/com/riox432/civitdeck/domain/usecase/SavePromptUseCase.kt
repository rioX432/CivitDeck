package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.repository.SavedPromptRepository

class SavePromptUseCase(private val repository: SavedPromptRepository) {
    suspend operator fun invoke(meta: ImageGenerationMeta, sourceImageUrl: String?) =
        repository.save(meta, sourceImageUrl)
}
