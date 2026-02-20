package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow

class ObserveModelCollectionsUseCase(private val repository: CollectionRepository) {
    operator fun invoke(modelId: Long): Flow<List<Long>> =
        repository.observeCollectionIdsForModel(modelId)
}
