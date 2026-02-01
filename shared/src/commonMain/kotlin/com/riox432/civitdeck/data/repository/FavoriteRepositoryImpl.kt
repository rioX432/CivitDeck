package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.FavoriteModelDao
import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.toDomain
import com.riox432.civitdeck.domain.model.toFavoriteEntity
import com.riox432.civitdeck.domain.repository.FavoriteRepository
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
