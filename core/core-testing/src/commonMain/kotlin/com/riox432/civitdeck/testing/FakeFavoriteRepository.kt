package com.riox432.civitdeck.testing

import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [FavoriteRepository] for ViewModel tests.
 *
 * Tracks the last toggled model and exposes mutable backing flows so tests can
 * drive favorite/observe state without a real database.
 */
class FakeFavoriteRepository(
    initialFavorites: List<FavoriteModelSummary> = emptyList(),
    isFavorite: Boolean = false,
) : FavoriteRepository {

    val favoritesFlow = MutableStateFlow(initialFavorites)
    val isFavoriteFlow = MutableStateFlow(isFavorite)

    var toggledModel: Model? = null
    var toggleCount: Int = 0
    var getAllFavoriteIdsCount: Int = 0

    override fun observeFavorites(): Flow<List<FavoriteModelSummary>> = favoritesFlow
    override fun observeIsFavorite(modelId: Long): Flow<Boolean> = isFavoriteFlow

    override suspend fun toggleFavorite(model: Model) {
        toggledModel = model
        toggleCount++
        isFavoriteFlow.value = !isFavoriteFlow.value
    }

    override suspend fun addFavorite(model: Model) {
        isFavoriteFlow.value = true
    }

    override suspend fun removeFavorite(modelId: Long) {
        isFavoriteFlow.value = false
    }

    override suspend fun getAllFavoriteIds(): Set<Long> {
        getAllFavoriteIdsCount++
        return favoritesFlow.value.map { it.id }.toSet()
    }

    override suspend fun getFavoriteTypeCounts(): Map<String, Int> = emptyMap()
}
