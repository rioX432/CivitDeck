package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoritesUseCase(private val repository: FavoriteRepository) {
    operator fun invoke(): Flow<List<FavoriteModelSummary>> = repository.observeFavorites()
}
