package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.currentTimeMillis
import com.riox432.civitdeck.data.local.dao.CollectionDao
import com.riox432.civitdeck.data.local.entity.CollectionEntity
import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.domain.model.toCollectionModelEntry
import com.riox432.civitdeck.domain.model.toFavoriteModelSummary
import com.riox432.civitdeck.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CollectionRepositoryImpl(
    private val dao: CollectionDao,
) : CollectionRepository {

    override fun observeCollections(): Flow<List<ModelCollection>> =
        dao.observeAllCollections().map { entities ->
            entities.map { entity ->
                ModelCollection(
                    id = entity.id,
                    name = entity.name,
                    isDefault = entity.isDefault,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                )
            }
        }

    override suspend fun createCollection(name: String): Long {
        val now = currentTimeMillis()
        return dao.insertCollection(
            CollectionEntity(name = name, createdAt = now, updatedAt = now),
        )
    }

    override suspend fun renameCollection(id: Long, name: String) {
        dao.renameCollection(id, name, currentTimeMillis())
    }

    override suspend fun deleteCollection(id: Long) {
        dao.deleteCollection(id)
    }

    override fun observeModelsInCollection(collectionId: Long): Flow<List<FavoriteModelSummary>> =
        dao.observeEntriesByCollection(collectionId).map { entities ->
            entities.map { it.toFavoriteModelSummary() }
        }

    override suspend fun addModelToCollection(collectionId: Long, model: Model) {
        dao.insertEntry(model.toCollectionModelEntry(collectionId, currentTimeMillis()))
    }

    override suspend fun removeModelFromCollection(collectionId: Long, modelId: Long) {
        dao.removeEntry(collectionId, modelId)
    }

    override fun observeCollectionIdsForModel(modelId: Long): Flow<List<Long>> =
        dao.observeCollectionIdsForModel(modelId)

    override suspend fun bulkRemoveModels(collectionId: Long, modelIds: List<Long>) {
        dao.removeEntries(collectionId, modelIds)
    }

    override suspend fun bulkMoveModels(
        fromCollectionId: Long,
        toCollectionId: Long,
        modelIds: List<Long>,
    ) {
        val entries = dao.getEntries(fromCollectionId, modelIds)
        val now = currentTimeMillis()
        val newEntries = entries.map { it.copy(collectionId = toCollectionId, addedAt = now) }
        dao.insertEntries(newEntries)
        dao.removeEntries(fromCollectionId, modelIds)
    }
}
