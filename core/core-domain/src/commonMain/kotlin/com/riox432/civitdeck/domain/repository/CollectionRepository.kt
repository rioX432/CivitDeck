package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelCollection
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun observeCollections(): Flow<List<ModelCollection>>
    suspend fun createCollection(name: String): Long
    suspend fun renameCollection(id: Long, name: String)
    suspend fun deleteCollection(id: Long)
    fun observeModelsInCollection(collectionId: Long): Flow<List<FavoriteModelSummary>>
    suspend fun addModelToCollection(collectionId: Long, model: Model)
    suspend fun removeModelFromCollection(collectionId: Long, modelId: Long)
    fun observeCollectionIdsForModel(modelId: Long): Flow<List<Long>>
    suspend fun bulkRemoveModels(collectionId: Long, modelIds: List<Long>)
    suspend fun bulkMoveModels(fromCollectionId: Long, toCollectionId: Long, modelIds: List<Long>)
}
