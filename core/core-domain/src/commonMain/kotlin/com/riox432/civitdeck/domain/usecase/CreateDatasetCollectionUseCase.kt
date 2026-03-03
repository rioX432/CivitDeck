package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository

class CreateDatasetCollectionUseCase(
    private val repository: DatasetCollectionRepository,
) {
    suspend operator fun invoke(name: String, description: String = ""): Long =
        repository.createCollection(name, description)
}
