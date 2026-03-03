package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository

class RenameDatasetCollectionUseCase(
    private val repository: DatasetCollectionRepository,
) {
    suspend operator fun invoke(id: Long, name: String) = repository.renameCollection(id, name)
}
