package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelNoteRepository

class GetAllPersonalTagsUseCase(private val repository: ModelNoteRepository) {
    suspend operator fun invoke(): List<String> = repository.getAllTags()
}
