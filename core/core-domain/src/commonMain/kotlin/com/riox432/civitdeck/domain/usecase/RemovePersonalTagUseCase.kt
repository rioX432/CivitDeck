package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelNoteRepository

class RemovePersonalTagUseCase(private val repository: ModelNoteRepository) {
    suspend operator fun invoke(modelId: Long, tag: String) =
        repository.removeTag(modelId, tag)
}
