package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow

class ObserveCollectionModelsUseCase(private val repository: CollectionRepository) {
    operator fun invoke(collectionId: Long): Flow<List<FavoriteModelSummary>> =
        repository.observeModelsInCollection(collectionId)
}
