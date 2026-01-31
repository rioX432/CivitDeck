package com.omooooori.civitdeck.domain.usecase

import com.omooooori.civitdeck.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class ObserveIsFavoriteUseCase(private val repository: FavoriteRepository) {
    operator fun invoke(modelId: Long): Flow<Boolean> = repository.observeIsFavorite(modelId)
}
