package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ExcludedTagRepository

class RemoveExcludedTagUseCase(private val repository: ExcludedTagRepository) {
    suspend operator fun invoke(tag: String) = repository.removeExcludedTag(tag)
}
