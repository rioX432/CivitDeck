package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.CollectionDao
import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.toCollectionModelEntry
import com.riox432.civitdeck.domain.model.toFavoriteModelSummary
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DEFAULT_COLLECTION_ID = 1L

class FavoriteRepositoryImpl(
    private val dao: CollectionDao,
) : FavoriteRepository {

    override fun observeFavorites(): Flow<List<FavoriteModelSummary>> =
        dao.observeEntriesByCollection(DEFAULT_COLLECTION_ID).map { entities ->
            entities.map { it.toFavoriteModelSummary() }
        }

    override fun observeIsFavorite(modelId: Long): Flow<Boolean> =
        dao.isFavorited(modelId)

    override suspend fun toggleFavorite(model: Model) {
        val exists = dao.isModelInCollection(DEFAULT_COLLECTION_ID, model.id)
        if (exists) {
            dao.removeEntry(DEFAULT_COLLECTION_ID, model.id)
        } else {
            dao.insertEntry(model.toCollectionModelEntry(DEFAULT_COLLECTION_ID, currentTimeMillis()))
        }
    }

    override suspend fun addFavorite(model: Model) {
        dao.insertEntry(model.toCollectionModelEntry(DEFAULT_COLLECTION_ID, currentTimeMillis()))
    }

    override suspend fun removeFavorite(modelId: Long) {
        dao.removeEntry(DEFAULT_COLLECTION_ID, modelId)
    }

    override suspend fun getAllFavoriteIds(): Set<Long> =
        dao.getAllFavoriteModelIds().toSet()

    override suspend fun getFavoriteTypeCounts(): Map<String, Int> =
        dao.getFavoriteTypeCounts().associate { it.type to it.cnt }
}
