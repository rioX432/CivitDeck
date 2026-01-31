package com.omooooori.civitdeck.data.repository

import com.omooooori.civitdeck.data.local.currentTimeMillis
import com.omooooori.civitdeck.data.local.dao.FavoriteModelDao
import com.omooooori.civitdeck.domain.model.FavoriteModelSummary
import com.omooooori.civitdeck.domain.model.Model
import com.omooooori.civitdeck.domain.model.toDomain
import com.omooooori.civitdeck.domain.model.toFavoriteEntity
import com.omooooori.civitdeck.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepositoryImpl(
    private val dao: FavoriteModelDao,
) : FavoriteRepository {

    override fun observeFavorites(): Flow<List<FavoriteModelSummary>> =
        dao.getAllAsFlow().map { entities -> entities.map { it.toDomain() } }

    override fun observeIsFavorite(modelId: Long): Flow<Boolean> =
        dao.isFavorite(modelId)

    override suspend fun toggleFavorite(model: Model) {
        val existing = dao.getById(model.id)
        if (existing != null) {
            dao.deleteById(model.id)
        } else {
            dao.insert(model.toFavoriteEntity(currentTimeMillis()))
        }
    }

    override suspend fun addFavorite(model: Model) {
        dao.insert(model.toFavoriteEntity(currentTimeMillis()))
    }

    override suspend fun removeFavorite(modelId: Long) {
        dao.deleteById(modelId)
    }
}
