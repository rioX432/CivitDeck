package com.omooooori.civitdeck.domain.usecase

import com.omooooori.civitdeck.domain.model.FavoriteModelSummary
import com.omooooori.civitdeck.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoritesUseCase(private val repository: FavoriteRepository) {
    operator fun invoke(): Flow<List<FavoriteModelSummary>> = repository.observeFavorites()
}
