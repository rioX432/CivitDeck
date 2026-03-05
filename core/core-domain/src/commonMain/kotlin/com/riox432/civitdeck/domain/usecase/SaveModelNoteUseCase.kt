package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelNoteRepository

class SaveModelNoteUseCase(private val repository: ModelNoteRepository) {
    suspend operator fun invoke(modelId: Long, noteText: String) =
        repository.saveNote(modelId, noteText)
}
