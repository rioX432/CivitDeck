package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import kotlinx.coroutines.flow.Flow

class ObserveDatasetCollectionsUseCase(
    private val repository: DatasetCollectionRepository,
) {
    operator fun invoke(): Flow<List<DatasetCollection>> = repository.observeCollections()
}
