package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.repository.CollectionRepository

class AddModelToCollectionUseCase(private val repository: CollectionRepository) {
    suspend operator fun invoke(collectionId: Long, model: Model) =
        repository.addModelToCollection(collectionId, model)
}
