package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ExcludedTagRepository

class GetExcludedTagsUseCase(private val repository: ExcludedTagRepository) {
    suspend operator fun invoke(): List<String> = repository.getExcludedTags()
}
