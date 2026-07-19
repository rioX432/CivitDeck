package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.InteractionEventEntity

@Dao
interface InteractionEventDao {
    @Insert
    suspend fun insert(event: InteractionEventEntity)

    @Query(
        "SELECT * FROM interaction_event WHERE timestamp >= :sinceMillis " +
            "ORDER BY timestamp DESC LIMIT :limit",
    )
    suspend fun getEventsSince(sinceMillis: Long, limit: Int = 500): List<InteractionEventEntity>

    @Query(
        "SELECT COUNT(*) FROM interaction_event " +
            "WHERE type = :type AND timestamp >= :sinceMillis",
    )
    suspend fun getCountByTypeSince(type: String, sinceMillis: Long): Int

    @Query("DELETE FROM interaction_event WHERE timestamp < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int

    @Query("DELETE FROM interaction_event")
    suspend fun deleteAll(): Int
}
