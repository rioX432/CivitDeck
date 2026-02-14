package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.data.local.LocalCacheDataSource
import com.riox432.civitdeck.data.local.entity.HiddenModelEntity
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import com.riox432.civitdeck.domain.repository.BrowsingHistoryRepository
import com.riox432.civitdeck.domain.repository.HiddenModelRepository
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveDefaultSortOrderUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<SortOrder> = repository.observeDefaultSortOrder()
}

class SetDefaultSortOrderUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(sort: SortOrder) = repository.setDefaultSortOrder(sort)
}

class ObserveDefaultTimePeriodUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<TimePeriod> = repository.observeDefaultTimePeriod()
}

class SetDefaultTimePeriodUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(period: TimePeriod) = repository.setDefaultTimePeriod(period)
}

class ObserveGridColumnsUseCase(private val repository: UserPreferencesRepository) {
    operator fun invoke(): Flow<Int> = repository.observeGridColumns()
}

class SetGridColumnsUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(columns: Int) = repository.setGridColumns(columns)
}

class GetHiddenModelsUseCase(private val repository: HiddenModelRepository) {
    suspend operator fun invoke(): List<HiddenModelEntity> = repository.getHiddenModels()
}

class ClearBrowsingHistoryUseCase(private val repository: BrowsingHistoryRepository) {
    suspend operator fun invoke() = repository.clearAll()
}

class ClearCacheUseCase(private val cacheDataSource: LocalCacheDataSource) {
    suspend operator fun invoke() = cacheDataSource.clearAll()
}
