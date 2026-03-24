package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow

class ObserveCollectionsUseCase(private val repository: CollectionRepository) {
    operator fun invoke(): Flow<List<ModelCollection>> = repository.observeCollections()
}
