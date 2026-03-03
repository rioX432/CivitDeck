package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import kotlinx.coroutines.flow.Flow

class ObserveDatasetImagesUseCase(
    private val repository: DatasetCollectionRepository,
) {
    operator fun invoke(datasetId: Long): Flow<List<DatasetImage>> =
        repository.observeImages(datasetId)
}
