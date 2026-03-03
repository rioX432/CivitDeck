package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ImageTagRepository

class GetTagSuggestionsUseCase(private val repository: ImageTagRepository) {
    suspend operator fun invoke(datasetId: Long, prefix: String): List<String> =
        repository.getTagSuggestions(datasetId, prefix)
}
