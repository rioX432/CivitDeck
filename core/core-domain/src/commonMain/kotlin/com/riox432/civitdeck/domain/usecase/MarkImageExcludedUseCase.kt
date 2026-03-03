package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository

class MarkImageExcludedUseCase(private val repository: DatasetCollectionRepository) {
    suspend operator fun invoke(imageId: Long, excluded: Boolean) =
        repository.markExcluded(imageId, excluded)
}
