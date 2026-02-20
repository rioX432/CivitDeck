package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface UserPreferencesRepository {
    fun observeNsfwFilterLevel(): Flow<NsfwFilterLevel>
    suspend fun setNsfwFilterLevel(level: NsfwFilterLevel)
    fun observeDefaultSortOrder(): Flow<SortOrder>
    suspend fun setDefaultSortOrder(sort: SortOrder)
    fun observeDefaultTimePeriod(): Flow<TimePeriod>
    suspend fun setDefaultTimePeriod(period: TimePeriod)
    fun observeGridColumns(): Flow<Int>
    suspend fun setGridColumns(columns: Int)
    fun observeApiKey(): Flow<String?>
    suspend fun setApiKey(apiKey: String?)
    suspend fun getApiKey(): String?
}
