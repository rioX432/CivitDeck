package com.omooooori.civitdeck.domain.repository

import com.omooooori.civitdeck.domain.model.FavoriteModelSummary
import com.omooooori.civitdeck.domain.model.Model
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun observeFavorites(): Flow<List<FavoriteModelSummary>>
    fun observeIsFavorite(modelId: Long): Flow<Boolean>
    suspend fun toggleFavorite(model: Model)
    suspend fun addFavorite(model: Model)
    suspend fun removeFavorite(modelId: Long)
}
