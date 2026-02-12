package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.HiddenModelEntity

@Dao
interface HiddenModelDao {
    @Query("SELECT modelId FROM hidden_models")
    suspend fun getAllIds(): List<Long>

    @Query("SELECT * FROM hidden_models ORDER BY hiddenAt DESC")
    suspend fun getAll(): List<HiddenModelEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: HiddenModelEntity)

    @Query("DELETE FROM hidden_models WHERE modelId = :modelId")
    suspend fun delete(modelId: Long)
}
