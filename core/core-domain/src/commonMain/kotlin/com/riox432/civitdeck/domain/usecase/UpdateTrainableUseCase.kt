package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository

class UpdateTrainableUseCase(private val repository: DatasetCollectionRepository) {
    suspend operator fun invoke(imageId: Long, trainable: Boolean) =
        repository.updateTrainable(imageId, trainable)
}
