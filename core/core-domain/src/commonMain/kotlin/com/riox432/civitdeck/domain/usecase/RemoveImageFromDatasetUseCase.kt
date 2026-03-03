package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository

class RemoveImageFromDatasetUseCase(
    private val repository: DatasetCollectionRepository,
) {
    suspend operator fun invoke(imageIds: List<Long>) = repository.removeImages(imageIds)
}
