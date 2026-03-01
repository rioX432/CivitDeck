package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import kotlinx.coroutines.flow.Flow

interface DisplayPreferencesRepository {
    fun observeDefaultSortOrder(): Flow<SortOrder>
    suspend fun setDefaultSortOrder(sort: SortOrder)
    fun observeDefaultTimePeriod(): Flow<TimePeriod>
    suspend fun setDefaultTimePeriod(period: TimePeriod)
    fun observeGridColumns(): Flow<Int>
    suspend fun setGridColumns(columns: Int)
    fun observeAccentColor(): Flow<AccentColor>
    suspend fun setAccentColor(color: AccentColor)
    fun observeAmoledDarkMode(): Flow<Boolean>
    suspend fun setAmoledDarkMode(enabled: Boolean)
}
