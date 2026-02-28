package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CollectionRepository

class BulkMoveModelsUseCase(private val repository: CollectionRepository) {
    suspend operator fun invoke(from: Long, to: Long, modelIds: List<Long>) =
        repository.bulkMoveModels(from, to, modelIds)
}
