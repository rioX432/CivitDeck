package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CollectionRepository

class RenameCollectionUseCase(private val repository: CollectionRepository) {
    suspend operator fun invoke(id: Long, name: String) = repository.renameCollection(id, name)
}
