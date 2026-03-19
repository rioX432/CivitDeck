package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.BrowsingHistoryDao
import com.riox432.civitdeck.data.local.dao.CollectionDao
import com.riox432.civitdeck.data.local.dao.SearchHistoryDao
import com.riox432.civitdeck.domain.model.BrowsingStats
import com.riox432.civitdeck.domain.model.CategoryStat
import com.riox432.civitdeck.domain.model.DailyViewCount
import com.riox432.civitdeck.domain.repository.AnalyticsRepository

private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000

class AnalyticsRepositoryImpl(
    private val browsingHistoryDao: BrowsingHistoryDao,
    private val collectionDao: CollectionDao,
    private val searchHistoryDao: SearchHistoryDao,
) : AnalyticsRepository {

    override suspend fun getBrowsingStats(): BrowsingStats {
        val now = com.riox432.civitdeck.data.local.currentTimeMillis()
        val sinceMillis = now - THIRTY_DAYS_MS

        val totalViews = browsingHistoryDao.count()
        val totalFavorites = collectionDao.getFavoriteTypeCounts().sumOf { it.cnt }
        val totalSearches = searchHistoryDao.count()

        val dailyViewCounts = browsingHistoryDao.getDailyViewCounts(sinceMillis).map {
            DailyViewCount(dayTimestamp = it.day, count = it.cnt)
        }

        val topModelTypes = browsingHistoryDao.getTopModelTypes().map {
            CategoryStat(name = it.name, count = it.cnt)
        }

        val topCreators = browsingHistoryDao.getTopCreators().map {
            CategoryStat(name = it.name, count = it.cnt)
        }

        val topSearchQueries = searchHistoryDao.getTopQueries().map {
            CategoryStat(name = it.name, count = it.cnt)
        }

        val averageViewDurationMs = browsingHistoryDao.getAverageViewDuration()

        return BrowsingStats(
            totalViews = totalViews,
            totalFavorites = totalFavorites,
            totalSearches = totalSearches,
            dailyViewCounts = dailyViewCounts,
            topModelTypes = topModelTypes,
            topCreators = topCreators,
            topSearchQueries = topSearchQueries,
            averageViewDurationMs = averageViewDurationMs,
        )
    }
}
