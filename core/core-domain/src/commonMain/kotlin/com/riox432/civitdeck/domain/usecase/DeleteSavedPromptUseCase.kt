package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.SavedPromptRepository

class DeleteSavedPromptUseCase(private val repository: SavedPromptRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
