package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository

class StoreImageDimensionsUseCase(private val repository: DatasetCollectionRepository) {
    suspend operator fun invoke(imageId: Long, width: Int, height: Int) =
        repository.updateDimensions(imageId, width, height)
}
