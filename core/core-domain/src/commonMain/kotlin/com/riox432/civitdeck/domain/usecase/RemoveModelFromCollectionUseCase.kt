package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CollectionRepository

class RemoveModelFromCollectionUseCase(private val repository: CollectionRepository) {
    suspend operator fun invoke(collectionId: Long, modelId: Long) =
        repository.removeModelFromCollection(collectionId, modelId)
}
