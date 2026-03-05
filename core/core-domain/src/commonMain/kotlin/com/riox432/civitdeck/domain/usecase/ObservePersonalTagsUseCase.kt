package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.PersonalTag
import com.riox432.civitdeck.domain.repository.ModelNoteRepository
import kotlinx.coroutines.flow.Flow

class ObservePersonalTagsUseCase(private val repository: ModelNoteRepository) {
    operator fun invoke(modelId: Long): Flow<List<PersonalTag>> =
        repository.observeTagsForModel(modelId)
}
