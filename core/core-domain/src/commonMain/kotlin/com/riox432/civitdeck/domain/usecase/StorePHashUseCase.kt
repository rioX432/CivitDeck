package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository

class StorePHashUseCase(private val repository: DatasetCollectionRepository) {
    suspend operator fun invoke(imageId: Long, pHash: String?) =
        repository.updatePHash(imageId, pHash)
}
