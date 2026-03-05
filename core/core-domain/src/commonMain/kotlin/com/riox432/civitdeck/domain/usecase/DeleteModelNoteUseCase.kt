package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelNoteRepository

class DeleteModelNoteUseCase(private val repository: ModelNoteRepository) {
    suspend operator fun invoke(modelId: Long) = repository.deleteNote(modelId)
}
