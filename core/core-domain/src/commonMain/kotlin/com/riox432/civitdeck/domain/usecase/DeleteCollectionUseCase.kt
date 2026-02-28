package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CollectionRepository

class DeleteCollectionUseCase(private val repository: CollectionRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteCollection(id)
}
