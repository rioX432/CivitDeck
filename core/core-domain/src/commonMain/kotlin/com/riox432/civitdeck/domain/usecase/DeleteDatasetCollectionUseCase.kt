package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository

class DeleteDatasetCollectionUseCase(
    private val repository: DatasetCollectionRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteCollection(id)
}
