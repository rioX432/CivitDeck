package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository

class GetNonTrainableImagesUseCase(private val repository: DatasetCollectionRepository) {
    suspend operator fun invoke(datasetId: Long): List<DatasetImage> =
        repository.getNonTrainableImages(datasetId)
}
