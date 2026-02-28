package com.riox432.civitdeck.feature.prompts.domain.usecase

import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.repository.SavedPromptRepository

class AutoSavePromptUseCase(private val repository: SavedPromptRepository) {
    suspend operator fun invoke(meta: ImageGenerationMeta, sourceImageUrl: String?) =
        repository.autoSave(meta, sourceImageUrl)
}
