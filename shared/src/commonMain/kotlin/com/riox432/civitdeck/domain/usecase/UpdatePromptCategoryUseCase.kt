package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.SavedPromptRepository

class UpdatePromptCategoryUseCase(private val repository: SavedPromptRepository) {
    suspend operator fun invoke(id: Long, category: String?) =
        repository.updateCategory(id, category)
}
