package com.riox432.civitdeck.domain.model

data class BrowsingStats(
    val totalViews: Int,
    val totalFavorites: Int,
    val totalSearches: Int,
    val dailyViewCounts: List<DailyViewCount>,
    val topModelTypes: List<CategoryStat>,
    val topCreators: List<CategoryStat>,
    val topSearchQueries: List<CategoryStat>,
    val averageViewDurationMs: Long? = null,
)

data class DailyViewCount(
    val dayTimestamp: Long,
    val count: Int,
)

data class CategoryStat(
    val name: String,
    val count: Int,
)
