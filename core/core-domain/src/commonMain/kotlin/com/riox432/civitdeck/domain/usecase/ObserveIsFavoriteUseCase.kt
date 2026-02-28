package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class ObserveIsFavoriteUseCase(private val repository: FavoriteRepository) {
    operator fun invoke(modelId: Long): Flow<Boolean> = repository.observeIsFavorite(modelId)
}
