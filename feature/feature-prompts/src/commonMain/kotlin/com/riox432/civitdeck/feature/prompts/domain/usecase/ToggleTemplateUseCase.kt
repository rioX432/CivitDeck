package com.riox432.civitdeck.feature.prompts.domain.usecase

import com.riox432.civitdeck.domain.repository.SavedPromptRepository

class ToggleTemplateUseCase(private val repository: SavedPromptRepository) {
    suspend operator fun invoke(id: Long, isTemplate: Boolean, templateName: String? = null) =
        repository.toggleTemplate(id, isTemplate, templateName)
}
