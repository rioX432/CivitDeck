package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.repository.ModelRepository

/**
 * Fetches a batch of models for swipe-based discovery.
 * Uses "Newest" sort order to surface fresh content.
 */
class GetDiscoveryModelsUseCase(private val repository: ModelRepository) {
    suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
    ): List<Model> = repository.getModels(
        sort = SortOrder.Newest,
        cursor = cursor,
        limit = limit,
    ).items
}
