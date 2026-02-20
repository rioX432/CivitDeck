package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CollectionRepository

class BulkRemoveModelsUseCase(private val repository: CollectionRepository) {
    suspend operator fun invoke(collectionId: Long, modelIds: List<Long>) =
        repository.bulkRemoveModels(collectionId, modelIds)
}
