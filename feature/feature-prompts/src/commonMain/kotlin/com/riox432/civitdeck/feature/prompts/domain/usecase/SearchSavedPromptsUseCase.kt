package com.riox432.civitdeck.feature.prompts.domain.usecase

import com.riox432.civitdeck.domain.model.SavedPrompt
import com.riox432.civitdeck.domain.repository.SavedPromptRepository
import kotlinx.coroutines.flow.Flow

class SearchSavedPromptsUseCase(private val repository: SavedPromptRepository) {
    operator fun invoke(query: String): Flow<List<SavedPrompt>> = repository.search(query)
}
