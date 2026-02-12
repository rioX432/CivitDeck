package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ExcludedTagRepository

class GetExcludedTagsUseCase(private val repository: ExcludedTagRepository) {
    suspend operator fun invoke(): List<String> = repository.getExcludedTags()
}

class AddExcludedTagUseCase(private val repository: ExcludedTagRepository) {
    suspend operator fun invoke(tag: String) = repository.addExcludedTag(tag)
}

class RemoveExcludedTagUseCase(private val repository: ExcludedTagRepository) {
    suspend operator fun invoke(tag: String) = repository.removeExcludedTag(tag)
}
