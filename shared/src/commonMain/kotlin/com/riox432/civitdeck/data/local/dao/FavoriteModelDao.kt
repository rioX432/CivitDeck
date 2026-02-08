package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.FavoriteModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteModelDao {
    @Query("SELECT * FROM favorite_models ORDER BY favoritedAt DESC")
    fun getAllAsFlow(): Flow<List<FavoriteModelEntity>>

    @Query("SELECT * FROM favorite_models WHERE id = :id")
    suspend fun getById(id: Long): FavoriteModelEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_models WHERE id = :id)")
    fun isFavorite(id: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteModelEntity)

    @Query("DELETE FROM favorite_models WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM favorite_models")
    suspend fun count(): Int

    @Query("SELECT id FROM favorite_models")
    suspend fun getAllIds(): List<Long>

    @Query("SELECT type, COUNT(*) as cnt FROM favorite_models GROUP BY type")
    suspend fun getTypeCounts(): List<TypeCount>
}

data class TypeCount(val type: String, val cnt: Int)
