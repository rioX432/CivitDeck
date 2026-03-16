package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.CollectionEntity
import com.riox432.civitdeck.data.local.entity.CollectionModelEntity
import kotlinx.coroutines.flow.Flow

data class TypeCount(val type: String, val cnt: Int)

data class CollectionWithCount(
    val id: Long,
    val name: String,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val modelCount: Int,
    val thumbnailUrl: String?,
)

@Suppress("TooManyFunctions")
@Dao
interface CollectionDao {
    // --- Collection CRUD ---

    @Query("SELECT * FROM collections ORDER BY isDefault DESC, name ASC")
    fun observeAllCollections(): Flow<List<CollectionEntity>>

    @Query(
        """
        SELECT
            c.id,
            c.name,
            c.isDefault,
            c.createdAt,
            c.updatedAt,
            COUNT(e.modelId) AS modelCount,
            (SELECT thumbnailUrl FROM collection_model_entries
                WHERE collectionId = c.id ORDER BY addedAt DESC LIMIT 1) AS thumbnailUrl
        FROM collections c
        LEFT JOIN collection_model_entries e ON e.collectionId = c.id
        GROUP BY c.id
        ORDER BY c.isDefault DESC, c.name ASC
        """,
    )
    fun observeAllCollectionsWithCount(): Flow<List<CollectionWithCount>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Query("UPDATE collections SET name = :name, updatedAt = :updatedAt WHERE id = :id AND isDefault = 0")
    suspend fun renameCollection(id: Long, name: String, updatedAt: Long): Int

    @Query("DELETE FROM collections WHERE id = :id AND isDefault = 0")
    suspend fun deleteCollection(id: Long): Int

    // --- Collection Model Entries ---

    @Query("SELECT * FROM collection_model_entries WHERE collectionId = :collectionId ORDER BY addedAt DESC")
    fun observeEntriesByCollection(collectionId: Long): Flow<List<CollectionModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CollectionModelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<CollectionModelEntity>)

    @Query("DELETE FROM collection_model_entries WHERE collectionId = :collectionId AND modelId = :modelId")
    suspend fun removeEntry(collectionId: Long, modelId: Long): Int

    @Query("DELETE FROM collection_model_entries WHERE collectionId = :collectionId AND modelId IN (:modelIds)")
    suspend fun removeEntries(collectionId: Long, modelIds: List<Long>): Int

    @Query(
        "SELECT * FROM collection_model_entries WHERE collectionId = :collectionId AND modelId IN (:modelIds)",
    )
    suspend fun getEntries(collectionId: Long, modelIds: List<Long>): List<CollectionModelEntity>

    // --- Backward-compat (favorites = collectionId 1) ---

    @Query("SELECT EXISTS(SELECT 1 FROM collection_model_entries WHERE collectionId = 1 AND modelId = :modelId)")
    fun isFavorited(modelId: Long): Flow<Boolean>

    @Query(
        "SELECT EXISTS(SELECT 1 FROM collection_model_entries " +
            "WHERE collectionId = :collectionId AND modelId = :modelId)",
    )
    suspend fun isModelInCollection(collectionId: Long, modelId: Long): Boolean

    @Query("SELECT modelId FROM collection_model_entries WHERE collectionId = 1")
    suspend fun getAllFavoriteModelIds(): List<Long>

    @Query("SELECT type, COUNT(*) as cnt FROM collection_model_entries WHERE collectionId = 1 GROUP BY type")
    suspend fun getFavoriteTypeCounts(): List<TypeCount>

    // --- Bulk queries (for backup) ---

    @Query("SELECT * FROM collections ORDER BY id ASC")
    suspend fun getAll(): List<CollectionEntity>

    @Query("SELECT * FROM collection_model_entries ORDER BY collectionId ASC, addedAt DESC")
    suspend fun getAllEntries(): List<CollectionModelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<CollectionEntity>)

    @Query("DELETE FROM collection_model_entries")
    suspend fun deleteAllEntries(): Int

    @Query("DELETE FROM collections WHERE isDefault = 0")
    suspend fun deleteAllNonDefault(): Int

    // --- Cross-collection queries ---

    @Query("SELECT collectionId FROM collection_model_entries WHERE modelId = :modelId")
    fun observeCollectionIdsForModel(modelId: Long): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM collection_model_entries WHERE collectionId = :collectionId")
    fun observeCollectionCount(collectionId: Long): Flow<Int>

    @Query(
        "SELECT thumbnailUrl FROM collection_model_entries " +
            "WHERE collectionId = :collectionId ORDER BY addedAt DESC LIMIT 1",
    )
    fun observeCollectionThumbnail(collectionId: Long): Flow<String?>
}
