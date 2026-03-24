package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ExcludedTagRepository

class AddExcludedTagUseCase(private val repository: ExcludedTagRepository) {
    suspend operator fun invoke(tag: String) = repository.addExcludedTag(tag)
}
