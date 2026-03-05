package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelNote
import com.riox432.civitdeck.domain.repository.ModelNoteRepository
import kotlinx.coroutines.flow.Flow

class ObserveModelNoteUseCase(private val repository: ModelNoteRepository) {
    operator fun invoke(modelId: Long): Flow<ModelNote?> =
        repository.observeNoteForModel(modelId)
}
