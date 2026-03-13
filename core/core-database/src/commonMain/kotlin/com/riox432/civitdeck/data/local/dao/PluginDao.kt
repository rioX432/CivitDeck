package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.PluginEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PluginDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PluginEntity)

    @Query("SELECT * FROM plugins ORDER BY installedAt DESC")
    fun observeAll(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins WHERE id = :pluginId")
    fun observeById(pluginId: String): Flow<PluginEntity?>

    @Query("SELECT * FROM plugins WHERE id = :pluginId")
    suspend fun getById(pluginId: String): PluginEntity?

    @Query("DELETE FROM plugins WHERE id = :pluginId")
    suspend fun delete(pluginId: String)

    @Query("UPDATE plugins SET state = :state, updatedAt = :updatedAt WHERE id = :pluginId")
    suspend fun updateState(pluginId: String, state: String, updatedAt: Long)

    @Query("UPDATE plugins SET configJson = :configJson, updatedAt = :updatedAt WHERE id = :pluginId")
    suspend fun updateConfig(pluginId: String, configJson: String, updatedAt: Long)
}
