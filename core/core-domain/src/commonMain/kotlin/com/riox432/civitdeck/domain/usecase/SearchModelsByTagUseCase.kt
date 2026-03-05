package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelNoteRepository

class SearchModelsByTagUseCase(private val repository: ModelNoteRepository) {
    suspend operator fun invoke(tag: String): List<Long> =
        repository.getModelIdsByTag(tag)
}
