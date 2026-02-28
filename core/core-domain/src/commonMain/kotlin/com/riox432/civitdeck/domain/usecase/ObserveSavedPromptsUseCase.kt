package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import kotlinx.coroutines.flow.Flow

class ObserveSavedPromptsUseCase(private val repository: SavedPromptRepository) {
    operator fun invoke(): Flow<List<SavedPrompt>> = repository.observeAll()
}
