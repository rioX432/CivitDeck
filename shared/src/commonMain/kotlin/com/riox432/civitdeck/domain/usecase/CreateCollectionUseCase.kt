package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CollectionRepository

class CreateCollectionUseCase(private val repository: CollectionRepository) {
    suspend operator fun invoke(name: String): Long = repository.createCollection(name)
}
