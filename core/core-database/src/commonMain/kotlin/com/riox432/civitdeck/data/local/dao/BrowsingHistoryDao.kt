package com.riox432.civitdeck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.riox432.civitdeck.data.local.entity.BrowsingHistoryEntity
import kotlinx.coroutines.flow.Flow

data class DayCount(val day: Long, val cnt: Int)

data class NameCount(val name: String, val cnt: Int)

@Dao
@Suppress("TooManyFunctions")
interface BrowsingHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BrowsingHistoryEntity)

    @Query("SELECT * FROM browsing_history ORDER BY viewedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<BrowsingHistoryEntity>

    @Query("SELECT * FROM browsing_history ORDER BY viewedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<BrowsingHistoryEntity>>

    @Query("DELETE FROM browsing_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT DISTINCT modelId FROM browsing_history ORDER BY viewedAt DESC LIMIT :limit")
    suspend fun getRecentModelIds(limit: Int = 50): List<Long>

    @Query("SELECT DISTINCT modelId FROM browsing_history")
    suspend fun getAllModelIds(): List<Long>

    @Query("SELECT COUNT(*) FROM browsing_history")
    suspend fun count(): Int

    @Query("DELETE FROM browsing_history")
    suspend fun deleteAll(): Int

    @Query(
        "SELECT (viewedAt / 86400000) * 86400000 AS day, COUNT(*) AS cnt " +
            "FROM browsing_history WHERE viewedAt >= :sinceMillis " +
            "GROUP BY day ORDER BY day ASC",
    )
    suspend fun getDailyViewCounts(sinceMillis: Long): List<DayCount>

    @Query(
        "SELECT modelType AS name, COUNT(*) AS cnt " +
            "FROM browsing_history GROUP BY modelType ORDER BY cnt DESC LIMIT :limit",
    )
    suspend fun getTopModelTypes(limit: Int = 10): List<NameCount>

    @Query(
        "SELECT creatorName AS name, COUNT(*) AS cnt " +
            "FROM browsing_history WHERE creatorName IS NOT NULL " +
            "GROUP BY creatorName ORDER BY cnt DESC LIMIT :limit",
    )
    suspend fun getTopCreators(limit: Int = 10): List<NameCount>

    @Query("DELETE FROM browsing_history WHERE viewedAt < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int

    @Query(
        "DELETE FROM browsing_history WHERE id NOT IN " +
            "(SELECT id FROM browsing_history ORDER BY viewedAt DESC LIMIT :maxCount)",
    )
    suspend fun deleteExcessEntries(maxCount: Int): Int

    @Query(
        "SELECT modelType AS name, COUNT(*) AS cnt " +
            "FROM browsing_history WHERE viewedAt >= :sinceMillis " +
            "GROUP BY modelType ORDER BY cnt DESC LIMIT :limit",
    )
    suspend fun getTypeCountsSince(sinceMillis: Long, limit: Int = 10): List<NameCount>

    @Query(
        "SELECT * FROM browsing_history WHERE viewedAt >= :sinceMillis " +
            "ORDER BY viewedAt DESC LIMIT :limit",
    )
    suspend fun getRecentSince(sinceMillis: Long, limit: Int = 200): List<BrowsingHistoryEntity>

    @Query(
        "SELECT creatorName AS name, COUNT(*) AS cnt " +
            "FROM browsing_history WHERE creatorName IS NOT NULL " +
            "AND viewedAt >= :sinceMillis " +
            "GROUP BY creatorName ORDER BY cnt DESC LIMIT :limit",
    )
    suspend fun getCreatorCountsSince(sinceMillis: Long, limit: Int = 10): List<NameCount>

    @Query("UPDATE browsing_history SET durationMs = :durationMs WHERE id = :id")
    suspend fun updateDuration(id: Long, durationMs: Long)

    @Query(
        "UPDATE browsing_history SET interactionType = :interactionType WHERE id = :id",
    )
    suspend fun updateInteractionType(id: Long, interactionType: String)

    @Query(
        "SELECT id FROM browsing_history WHERE modelId = :modelId " +
            "ORDER BY viewedAt DESC LIMIT 1",
    )
    suspend fun getLatestIdForModel(modelId: Long): Long?

    @Query(
        "SELECT AVG(durationMs) FROM browsing_history WHERE durationMs IS NOT NULL",
    )
    suspend fun getAverageViewDuration(): Long?
}
